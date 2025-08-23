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
import one.jfr.event.Event;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.CPUInformation;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.InitialEnvironmentVariable;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.InitialSystemProperty;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.JVMInformation;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.OSInformation;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.StackFrame;
import org.bithon.component.commons.forbidden.SuppressForbidden;
import org.bithon.shaded.net.bytebuddy.jar.asm.Type;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

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

    public static JfrFileReader createReader(String filePath, int maxRetries) throws IOException {
        IOException lastException = null;

        for (int i = 0; i < maxRetries; i++) {
            try {
                JfrReader delegate = new JfrReader(filePath);
                delegate.registerEvent("jdk.CPUInformation", CPUInformation.class);
                delegate.registerEvent("jdk.InitialSystemProperty", InitialSystemProperty.class);
                delegate.registerEvent("jdk.InitialEnvironmentVariable", InitialEnvironmentVariable.class);
                delegate.registerEvent("jdk.OSInformation", OSInformation.class);
                delegate.registerEvent("jdk.JVMInformation", JVMInformation.class);
                return new JfrFileReader(delegate);
            } catch (IOException e) {
                lastException = e;

                // If it's an incomplete file error, wait and retry
                if (e.getMessage().contains("Incomplete") || e.getMessage().contains("Invalid")) {
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new IOException("Interrupted while waiting to retry", ie);
                    }
                } else {
                    // Unknown exception, rethrow it directly
                    throw e;
                }
            }
        }

        throw new IOException("Failed to process JFR file after " + maxRetries + " retries", lastException);
    }

    @SuppressForbidden
    public static void dumpRawEvents(File f) throws IOException {
        try (JfrFileReader jfr = createReader(f.getAbsolutePath(), 3)) {
            for (Event jfrEvent; (jfrEvent = jfr.readEvent()) != null; ) {
                System.out.println(jfrEvent);
            }
        }
    }

    public static void main(String[] args) throws IOException {
        File f = new File("/Users/frank.chenling/source/open/bithon/agent/agent-distribution/target/agent-distribution/agent-distribution/tools/async-profiler/macos/bin/tmp.jfr");

        //dumpRawEvents(f);

        JfrFileConsumer.consume(f, new JfrFileConsumer.EventConsumer() {
            @Override
            public void onStart() {
                System.out.println("JFR reading started");
            }

            @Override
            public void onEvent(ProfilingEvent event) {
                if (event.getEventCase() == ProfilingEvent.EventCase.CPULOAD) {
                    System.out.println(event);
                }
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

    public List<StackFrame> getStackTrace(int stackTraceId) {
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

    /**
     * Convert JFR event time to epoch milliseconds.
     * JFR event times are in ticks, which is somehow based on seconds
     */
    public long toEpochMilliseconds(long eventTimeTicks) {
        long elapsedTicks = eventTimeTicks - delegate.startTicks;

        // Convert ticks to seconds
        long elapsedMilliseconds = elapsedTicks * 1000L / delegate.ticksPerSec;

        // Add to the start time in seconds
        return delegate.startNanos / 1_000_000L + elapsedMilliseconds;
    }

    public String getThreadName(int tid) {
        String threadName = delegate.threads.get(tid);
        return threadName != null ? threadName : "Unknown Thread";
    }
}
