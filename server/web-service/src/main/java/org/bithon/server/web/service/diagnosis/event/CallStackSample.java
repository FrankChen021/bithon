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

package org.bithon.server.web.service.diagnosis.event;


import aj.org.objectweb.asm.Type;
import one.jfr.ClassRef;
import one.jfr.JfrReader;
import one.jfr.MethodRef;
import one.jfr.event.ExecutionSample;
import org.bithon.server.web.service.diagnosis.StackFrame;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import static one.convert.Frame.TYPE_CPP;
import static one.convert.Frame.TYPE_KERNEL;
import static one.convert.Frame.TYPE_NATIVE;

/**
 * An extent to ExecutionSample
 *
 * @author frank.chen021@outlook.com
 * @date 14/7/25 10:59 am
 */
public class CallStackSample implements IEvent {
    public final long time;
    public final int threadId;
    public final String threadName;
    public final int threadState;
    public final List<StackFrame> stackTrace;
    public final int samples;

    protected CallStackSample(JfrReader jfr, ExecutionSample event) {
        this.time = TimeConverter.toEpochNano(jfr, event.time);

        one.jfr.StackTrace stackTrace = jfr.stackTraces.get(event.stackTraceId);
        this.stackTrace = toStackTrace(jfr, stackTrace);

        this.threadId = event.tid;
        this.threadName = jfr.threads.get(event.tid);
        this.threadState = event.threadState;

        this.samples = event.samples;
    }

    @Override
    public String toString() {
        return "CallStackSample{" +
               ", stackTrace=" + (stackTrace != null ? stackTrace.toString() : "") +
               '}';
    }

    public static CallStackSample toCallStackSample(JfrReader jfr, ExecutionSample event) {
        if (event.stackTraceId == 0) {
            return null; // no stack trace
        }
        return new CallStackSample(jfr, event);
    }

    public static List<StackFrame> toStackTrace(JfrReader jfr,
                                                one.jfr.StackTrace stackTrace) {
        List<StackFrame> frames = new ArrayList<>();
        for (int i = 0; i < stackTrace.methods.length; i++) {
            frames.add(toStackFrame(jfr, stackTrace, i));
        }
        return frames;
    }

    private static StackFrame toStackFrame(JfrReader jfr, one.jfr.StackTrace stackTrace, int frameIndex) {
        long methodId = stackTrace.methods[frameIndex];

        StackFrame frame = new StackFrame();

        int location = -1;
        if ((location = stackTrace.locations[frameIndex] >>> 16) != 0) {
            frame.location = location;
        } else if ((location = stackTrace.locations[frameIndex] & 0xffff) != 0) {
            frame.location = location;
        } else {
            frame.location = location;
        }

        frame.type = stackTrace.types[frameIndex];

        MethodRef method = jfr.methods.get(methodId);
        if (method == null) {
            frame.method = "Unknown";
            return frame;
        }

        // NOTE: MethodRef does not store modifier now

        ClassRef cls = jfr.classes.get(method.cls);
        byte[] className = jfr.symbols.get(cls.name);
        byte[] methodName = jfr.symbols.get(method.name);
        byte[] sig = jfr.symbols.get(method.sig);
        if (className == null || className.length == 0) {
            frame.method = new String(methodName, StandardCharsets.UTF_8);
            return frame;
        } else {
            String classStr = toJavaClassName(className, 0, true);
            if (methodName == null || methodName.length == 0) {
                frame.typeName = classStr;
                frame.method = "Unknown";
                return frame;
            }
            String methodStr = new String(methodName, StandardCharsets.UTF_8);
            String sigStr = sig != null ? new String(sig, StandardCharsets.UTF_8) : "";

            frame.typeName = classStr;
            frame.method = methodStr;

            if (!sigStr.isEmpty()) {
                Type methodType = Type.getMethodType(sigStr);
                Type[] argTypes = methodType.getArgumentTypes();
                frame.parameters = Stream.of(argTypes)
                                         .map(Type::getClassName)
                                         .toArray(String[]::new);
                frame.returnType = methodType.getReturnType().getClassName();
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
