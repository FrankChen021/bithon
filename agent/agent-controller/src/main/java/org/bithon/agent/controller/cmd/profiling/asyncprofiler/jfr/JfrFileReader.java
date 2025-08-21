/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr;


import one.jfr.ClassRef;
import one.jfr.JfrReader;
import one.jfr.MethodRef;
import one.jfr.event.AllocationSample;
import one.jfr.event.CPULoad;
import one.jfr.event.Event;
import one.jfr.event.ExecutionSample;
import one.jfr.event.GCHeapSummary;
import one.jfr.event.MallocEvent;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.CPUInformation;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.InitialEnvironmentVariable;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.InitialSystemProperty;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.JVMInformation;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.OSInformation;
import org.bithon.agent.rpc.brpc.profiling.CallStackSample;
import org.bithon.agent.rpc.brpc.profiling.HeapSummary;
import org.bithon.agent.rpc.brpc.profiling.Malloc;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.StackFrame;
import org.bithon.agent.rpc.brpc.profiling.SystemProperties;
import org.bithon.component.commons.forbidden.SuppressForbidden;
import org.bithon.shaded.net.bytebuddy.jar.asm.Type;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static one.convert.Frame.TYPE_CPP;
import static one.convert.Frame.TYPE_KERNEL;
import static one.convert.Frame.TYPE_NATIVE;

/**
 * @author frank.chen021@outlook.com
 * @date 4/8/25 11:07 am
 */
public class JfrFileReader implements Closeable {
    private final JfrReader delegate;

    public JfrFileReader(JfrReader delegate) {
        this.delegate = delegate;
    }

    public Event readEvent() throws IOException {
        return delegate.readEvent();
    }

    @Override
    public void close() throws IOException {
        delegate.close();
    }

    public static void read(File jfrFile, JfrEventConsumer eventConsumer) throws IOException {
        try (JfrFileReader reader = createJfrReader(jfrFile.getAbsolutePath())) {
            // Use TreeMap to ensure the system properties are sorted by key
            final Map<String, String> systemProperties = new TreeMap<>();
            eventConsumer.onStart();
            {
                for (Event jfrEvent; !eventConsumer.isCancelled() && (jfrEvent = reader.readEvent()) != null; ) {
                    ProfilingEvent responseEvent = null;
                    if (jfrEvent instanceof ExecutionSample) {
                        responseEvent = ProfilingEvent.newBuilder()
                                                      .setCallStackSample(reader.toCallStackSample((ExecutionSample) jfrEvent))
                                                      .build();

                    } else if (jfrEvent instanceof CPULoad) {
                        org.bithon.agent.rpc.brpc.profiling.CPULoad cpuLoad = org.bithon.agent.rpc.brpc.profiling.CPULoad.newBuilder()
                                                                                                                         .setTime(reader.toEpochNano(jfrEvent.time))
                                                                                                                         .setUser(((CPULoad) jfrEvent).jvmUser)
                                                                                                                         .setSystem(((CPULoad) jfrEvent).jvmSystem)
                                                                                                                         .setMachine(((CPULoad) jfrEvent).machineTotal)
                                                                                                                         .build();
                        responseEvent = ProfilingEvent.newBuilder()
                                                      .setCpuLoad(cpuLoad)
                                                      .build();

                    } else if (jfrEvent instanceof InitialSystemProperty) {

                        systemProperties.put(((InitialSystemProperty) jfrEvent).key, ((InitialSystemProperty) jfrEvent).value);

                    } else if (jfrEvent instanceof GCHeapSummary) {
                        GCHeapSummary heap = (GCHeapSummary) jfrEvent;
                        responseEvent = ProfilingEvent.newBuilder()
                                                      .setHeapSummary(HeapSummary.newBuilder()
                                                                                 .setTime(reader.toEpochNano(jfrEvent.time))
                                                                                 .setGcId(heap.gcId)
                                                                                 .setUsed(heap.used)
                                                                                 .setCommitted(heap.committed)
                                                                                 .setReserved(heap.reserved)
                                                                                 .build())
                                                      .build();

                    } else if (jfrEvent instanceof MallocEvent) { // The native memory allocation event
                        MallocEvent mallocEvent = (MallocEvent) jfrEvent;
                        if (mallocEvent.stackTraceId == 0 || mallocEvent.size == 0) {
                            continue;
                        }

                        Malloc malloc = Malloc.newBuilder()
                                              .setTime(reader.toEpochNano(mallocEvent.time))
                                              .setThreadId(mallocEvent.tid)
                                              .setThreadName(reader.getThreadName(mallocEvent.tid))
                                              .setSize(mallocEvent.size)
                                              .addAllStackTrace(reader.toStackTrace(mallocEvent.stackTraceId))
                                              .build();
                        responseEvent = ProfilingEvent.newBuilder().setMalloc(malloc).build();

                    } else if (jfrEvent instanceof AllocationSample) {
                        AllocationSample allocationSample = (AllocationSample) jfrEvent;
                        if (allocationSample.stackTraceId == 0) {
                            continue;
                        }

                        org.bithon.agent.rpc.brpc.profiling.AllocationSample sample = org.bithon.agent.rpc.brpc.profiling.AllocationSample.newBuilder()
                                                                                                                                          .setTime(reader.toEpochNano(allocationSample.time))
                                                                                                                                          .setThreadId(allocationSample.tid)
                                                                                                                                          .setThreadName(reader.getThreadName(allocationSample.tid))
                                                                                                                                          .addAllStackTrace(reader.toStackTrace(allocationSample.stackTraceId))
                                                                                                                                          .setAllocationSize(allocationSample.allocationSize)
                                                                                                                                          .setTlabSize(allocationSample.tlabSize)
                                                                                                                                          .setClazz(reader.getClassName(allocationSample.classId))
                                                                                                                                          .build();
                        responseEvent = ProfilingEvent.newBuilder().setAllocationSample(sample).build();

                    }
                    if (responseEvent != null) {
                        eventConsumer.onEvent(responseEvent);
                    }
                }
            }
            if (!systemProperties.isEmpty()) {
                SystemProperties.Builder props = SystemProperties.newBuilder()
                                                                 .putAllProperties(systemProperties);
                eventConsumer.onEvent(ProfilingEvent.newBuilder()
                                                    .setSystemProperties(props)
                                                    .build());
            }
        } finally {
            try {
                eventConsumer.onComplete();
            } catch (Exception ignored) {
            }
        }
    }

    private static JfrFileReader createJfrReader(String filePath) throws IOException {
        JfrReader delegate = new JfrReader(filePath);
        delegate.registerEvent("jdk.CPUInformation", CPUInformation.class);
        delegate.registerEvent("jdk.InitialSystemProperty", InitialSystemProperty.class);
        delegate.registerEvent("jdk.InitialEnvironmentVariable", InitialEnvironmentVariable.class);
        delegate.registerEvent("jdk.OSInformation", OSInformation.class);
        delegate.registerEvent("jdk.JVMInformation", JVMInformation.class);
        return new JfrFileReader(delegate);
    }

    @SuppressForbidden
    public static void dump(File f) throws IOException {
        try (JfrFileReader jfr = createJfrReader(f.getAbsolutePath())) {
            for (Event jfrEvent; (jfrEvent = jfr.readEvent()) != null; ) {
                System.out.println(jfrEvent);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/Users/frank.chenling/source/open/bithon/agent/agent-distribution/tools/async-profiler/macos/bin/output.jfr");
        read(f, new JfrEventConsumer() {
            @Override
            public void onStart() {
                System.out.println("JFR reading started");
            }

            @Override
            public void onEvent(ProfilingEvent event) {
                System.out.println(event);
            }

            @Override
            public void onComplete() {
                System.out.println("JFR reading completed");
            }

            @Override
            public boolean isCancelled() {
                return false;
            }
        });
    }

    public CallStackSample toCallStackSample(ExecutionSample event) {
        List<StackFrame> frames = toStackTrace(event.stackTraceId);
        if (frames == null) {
            return null;
        }
        CallStackSample.Builder builder = CallStackSample.newBuilder()
                                                         .setTime(TimeConverter.toEpochNano(this.delegate, event.time))
                                                         .setThreadId(event.tid)
                                                         .setThreadName(this.delegate.threads.get(event.tid))
                                                         .setThreadState(event.threadState)
                                                         .addAllStackTrace(frames)
                                                         .setSamples(event.samples);
        return builder.build();
    }

    private List<StackFrame> toStackTrace(int stackTraceId) {
        if (stackTraceId == 0) {
            return null;
        }

        one.jfr.StackTrace stackTrace = delegate.stackTraces.get(stackTraceId);
        List<StackFrame> frames = new ArrayList<>();
        for (int i = 0; i < stackTrace.methods.length; i++) {
            frames.add(toStackFrame(stackTrace, i).build());
        }
        return frames;
    }

    private StackFrame.Builder toStackFrame(one.jfr.StackTrace stackTrace, int frameIndex) {
        long methodId = stackTrace.methods[frameIndex];

        StackFrame.Builder stackFrame = StackFrame.newBuilder();

        int location;
        if ((location = stackTrace.locations[frameIndex] >>> 16) != 0) {
            stackFrame.setLocation(location);
        } else if ((location = stackTrace.locations[frameIndex] & 0xffff) != 0) {
            stackFrame.setLocation(location);
        } else {
            stackFrame.setLocation(location);
        }

        stackFrame.setType(stackTrace.types[frameIndex]);

        MethodRef method = this.delegate.methods.get(methodId);
        if (method == null) {
            stackFrame.setMethod("Unknown");
            return stackFrame;
        }

        String className = getClassName(method.cls);
        if (!className.isEmpty()) {
            stackFrame.setTypeName(className);
        }

        byte[] methodName = this.delegate.symbols.get(method.name);
        if (methodName == null || methodName.length == 0) {
            stackFrame.setTypeName(className);
            stackFrame.setMethod("Unknown");
            return stackFrame;
        }
        stackFrame.setTypeName(className);
        stackFrame.setMethod(new String(methodName, StandardCharsets.UTF_8));

        byte[] sig = this.delegate.symbols.get(method.sig);
        String sigStr = sig != null ? new String(sig, StandardCharsets.UTF_8) : "";
        if (!sigStr.isEmpty()) {
            Type methodType = Type.getMethodType(sigStr);
            Type[] argTypes = methodType.getArgumentTypes();
            for (Type argType : argTypes) {
                stackFrame.addParameters(argType.getClassName());
            }
            stackFrame.setReturnType(methodType.getReturnType().getClassName());
        }
        return stackFrame;
    }

    public String getClassName(long classId) {
        ClassRef clazzRef = delegate.classes.get(classId);
        if (clazzRef == null) {
            return "";
        }
        byte[] className = delegate.symbols.get(clazzRef.name);
        if (className == null || className.length == 0) {
            return "";
        }
        return toJavaClassName(className, 0, true);
    }

    private static String toJavaClassName(byte[] symbol, int start, boolean dotted) {
        int end = symbol.length;
        if (start > 0) {
            switch (symbol[start]) {
                case 'B':
                    return "byte";
                case 'C':
                    return "char";
                case 'S':
                    return "short";
                case 'I':
                    return "int";
                case 'J':
                    return "long";
                case 'Z':
                    return "boolean";
                case 'F':
                    return "float";
                case 'D':
                    return "double";
                case 'L':
                    start++;
                    end--;
            }
        }

        //if (args.norm) {
        for (int i = end - 2; i > start; i--) {
            if (symbol[i] == '/' || symbol[i] == '.') {
                if (symbol[i + 1] >= '0' && symbol[i + 1] <= '9') {
                    end = i;
                    if (i > start + 19 && symbol[i - 19] == '+' && symbol[i - 18] == '0') {
                        // Original JFR transforms lambda names to something like
                        // pkg.ClassName$$Lambda+0x00007f8177090218/543846639
                        end = i - 19;
                    }
                }
                break;
            }
        }
        //}

        //if (args.simple) {
        //for (int i = end - 2; i >= start; i--) {
        //    if (symbol[i] == '/' && (symbol[i + 1] < '0' || symbol[i + 1] > '9')) {
        //        start = i + 1;
        //        break;
        //    }
        //}
        //}

        String s = new String(symbol, start, end - start, StandardCharsets.UTF_8);
        return dotted ? s.replace('/', '.') : s;
    }

    protected static boolean isNativeFrame(JfrReader jfr, byte methodType) {
        // In JDK Flight Recorder, TYPE_NATIVE denotes Java native methods,
        // while in async-profiler, TYPE_NATIVE is for C methods
        return methodType == TYPE_NATIVE && jfr.getEnumValue("jdk.types.FrameType", TYPE_KERNEL) != null ||
               methodType == TYPE_CPP ||
               methodType == TYPE_KERNEL;
    }

    private long toEpochNano(long eventTime) {
        return delegate.startNanos + ((eventTime - delegate.startTicks) / delegate.ticksPerSec);
    }

    public String getThreadName(int tid) {
        String threadName = delegate.threads.get(tid);
        return threadName != null ? threadName : "Unknown Thread";
    }
}
