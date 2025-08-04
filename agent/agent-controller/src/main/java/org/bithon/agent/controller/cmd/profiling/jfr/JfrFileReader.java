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

package org.bithon.agent.controller.cmd.profiling.jfr;


import one.jfr.ClassRef;
import one.jfr.JfrReader;
import one.jfr.MethodRef;
import one.jfr.event.CPULoad;
import one.jfr.event.Event;
import one.jfr.event.ExecutionSample;
import one.jfr.event.GCHeapSummary;
import org.bithon.agent.controller.cmd.profiling.jfr.event.CPUInformation;
import org.bithon.agent.controller.cmd.profiling.jfr.event.InitialEnvironmentVariable;
import org.bithon.agent.controller.cmd.profiling.jfr.event.InitialSystemProperty;
import org.bithon.agent.controller.cmd.profiling.jfr.event.JVMInformation;
import org.bithon.agent.controller.cmd.profiling.jfr.event.OSInformation;
import org.bithon.agent.rpc.brpc.profiling.CPUUsage;
import org.bithon.agent.rpc.brpc.profiling.CallStackSample;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.StackFrame;
import org.bithon.agent.rpc.brpc.profiling.SystemProperties;
import org.bithon.component.commons.forbidden.SuppressForbidden;
import org.bithon.shaded.net.bytebuddy.jar.asm.Type;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static one.convert.Frame.TYPE_CPP;
import static one.convert.Frame.TYPE_KERNEL;
import static one.convert.Frame.TYPE_NATIVE;

/**
 * @author frank.chen021@outlook.com
 * @date 4/8/25 11:07 am
 */
public class JfrFileReader {
    public static void read(File jfrFile, JfrEventConsumer eventConsumer) throws IOException {
        try (JfrReader jfr = createJfrReader(jfrFile.getAbsolutePath())) {
            final Map<String, String> systemProperties = new HashMap<>();
            eventConsumer.onStart();
            {
                for (Event jfrEvent; (jfrEvent = jfr.readEvent()) != null; ) {
                    ProfilingEvent responseEvent = null;
                    if (jfrEvent instanceof ExecutionSample) {
                        responseEvent = ProfilingEvent.newBuilder()
                                                      .setCallStackSample(toCallStackSample(jfr, (ExecutionSample) jfrEvent))
                                                      .build();
                    } else if (jfrEvent instanceof CPULoad) {
                        CPUUsage cpuUsage = CPUUsage.newBuilder()
                                                    .setTime(TimeConverter.toEpochNano(jfr, jfrEvent.time))
                                                    .setUser(((CPULoad) jfrEvent).jvmUser)
                                                    .setSystem(((CPULoad) jfrEvent).jvmSystem)
                                                    .setMachine(((CPULoad) jfrEvent).machineTotal)
                                                    .build();
                        responseEvent = ProfilingEvent.newBuilder()
                                                      .setCpuUsage(cpuUsage)
                                                      .build();
                    } else if (jfrEvent instanceof InitialSystemProperty) {
                        systemProperties.put(((InitialSystemProperty) jfrEvent).key,
                                             ((InitialSystemProperty) jfrEvent).value);
                    } else if (jfrEvent instanceof GCHeapSummary) {
                        GCHeapSummary heap = (GCHeapSummary) jfrEvent;
                        responseEvent = ProfilingEvent.newBuilder()
                                                      .setHeapSummary(org.bithon.agent.rpc.brpc.profiling.GCHeapSummary.newBuilder()
                                                                                                                       .setTime(TimeConverter.toEpochNano(jfr, jfrEvent.time))
                                                                                                                       .setGcId(heap.gcId)
                                                                                                                       .setUsed(heap.used)
                                                                                                                       .setCommitted(heap.committed)
                                                                                                                       .setReserved(heap.reserved)
                                                                                                                       .build())
                                                      .build();
                    }

                                /*
                    String eventType = jfrEvent.getEventType().getName();
                    return JdkTypeIDs.CPU_LOAD.equals(eventType)
                           || JdkTypeIDs.EXECUTION_SAMPLE.equals(eventType)
                           || JdkTypeIDs.SYSTEM_PROPERTIES.equals(eventType);
                     */
                    if (responseEvent != null) {
                        eventConsumer.onEvent(responseEvent);
                    }
                }
            }
            if (!systemProperties.isEmpty()) {
                SystemProperties.Builder props = SystemProperties.newBuilder()
                                                                 .putAllProperties(systemProperties);
                eventConsumer.onEvent(ProfilingEvent.newBuilder().setSystemProperties(props).build());
            }
        } finally {
            try {
                eventConsumer.onComplete();
            } catch (Exception ignored) {
            }
        }
    }

    private static JfrReader createJfrReader(String filePath) throws IOException {
        JfrReader jfrReader = new JfrReader(filePath);
        jfrReader.registerEvent("jdk.CPUInformation", CPUInformation.class);
        jfrReader.registerEvent("jdk.InitialSystemProperty", InitialSystemProperty.class);
        jfrReader.registerEvent("jdk.InitialEnvironmentVariable", InitialEnvironmentVariable.class);
        jfrReader.registerEvent("jdk.OSInformation", OSInformation.class);
        jfrReader.registerEvent("jdk.JVMInformation", JVMInformation.class);
        return jfrReader;
    }

    @SuppressForbidden
    public static void dump(File f) throws IOException {
        read(f, new JfrEventConsumer() {
            @Override
            public void onStart() {
            }

            @Override
            public void onEvent(ProfilingEvent event) {
                if (event.getEventCase() != ProfilingEvent.EventCase.CALLSTACKSAMPLE && event.getEventCase() != ProfilingEvent.EventCase.SYSTEMPROPERTIES) {
                    System.out.println(event);
                }
            }

            @Override
            public void onComplete() {
            }
        });
    }

    public static void main(String[] args) throws IOException {
        dump(new File("/Users/frank.chenling/source/open/bithon/agent/agent-distribution/tools/async-profiler/macos/bin/20250804-114138.jfr"));
    }

    public static CallStackSample toCallStackSample(JfrReader jfr, ExecutionSample event) {
        if (event.stackTraceId == 0) {
            return null; // no stack trace
        }

        long time = TimeConverter.toEpochNano(jfr, event.time);
        int threadId = event.tid;
        String threadName = jfr.threads.get(event.tid);

        one.jfr.StackTrace stackTrace = jfr.stackTraces.get(event.stackTraceId);
        List<StackFrame> frames = toStackTrace(jfr, stackTrace);

        CallStackSample.Builder builder = CallStackSample.newBuilder()
                                                         .setTime(time)
                                                         .setThreadId(threadId)
                                                         .setThreadName(threadName)
                                                         .setThreadState(event.threadState)
                                                         .addAllStackTrace(frames)
                                                         .setSamples(event.samples);
        return builder.build();
    }

    public static List<StackFrame> toStackTrace(JfrReader jfr,
                                                one.jfr.StackTrace stackTrace) {
        List<StackFrame> frames = new ArrayList<>();
        for (int i = 0; i < stackTrace.methods.length; i++) {
            frames.add(toStackFrame(jfr, stackTrace, i).build());
        }
        return frames;
    }

    private static StackFrame.Builder toStackFrame(JfrReader jfr, one.jfr.StackTrace stackTrace, int frameIndex) {
        long methodId = stackTrace.methods[frameIndex];

        StackFrame.Builder frame = StackFrame.newBuilder();

        int location;
        if ((location = stackTrace.locations[frameIndex] >>> 16) != 0) {
            frame.setLocation(location);
        } else if ((location = stackTrace.locations[frameIndex] & 0xffff) != 0) {
            frame.setLocation(location);
        } else {
            frame.setLocation(location);
        }

        frame.setType(stackTrace.types[frameIndex]);

        MethodRef method = jfr.methods.get(methodId);
        if (method == null) {
            frame.setMethod("Unknown");
            return frame;
        }

        // NOTE: MethodRef does not store modifier now

        ClassRef cls = jfr.classes.get(method.cls);
        byte[] className = jfr.symbols.get(cls.name);
        byte[] methodName = jfr.symbols.get(method.name);
        byte[] sig = jfr.symbols.get(method.sig);
        if (className == null || className.length == 0) {
            frame.setMethod(new String(methodName, StandardCharsets.UTF_8));
            return frame;
        } else {
            String classStr = toJavaClassName(className, 0, true);
            if (methodName == null || methodName.length == 0) {
                frame.setTypeName(classStr);
                frame.setMethod("Unknown");
                return frame;
            }
            String methodStr = new String(methodName, StandardCharsets.UTF_8);
            String sigStr = sig != null ? new String(sig, StandardCharsets.UTF_8) : "";

            frame.setTypeName(classStr);
            frame.setMethod(methodStr);
            if (!sigStr.isEmpty()) {
                Type methodType = Type.getMethodType(sigStr);
                Type[] argTypes = methodType.getArgumentTypes();
                for (Type argType : argTypes) {
                    frame.addParameters(argType.getClassName());
                }
                frame.setReturnType(methodType.getReturnType().getClassName());
            }
            return frame;
        }
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
}
