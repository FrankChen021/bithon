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


import one.jfr.event.AllocationSample;
import one.jfr.event.CPULoad;
import one.jfr.event.ContendedLock;
import one.jfr.event.Event;
import one.jfr.event.ExecutionSample;
import one.jfr.event.GCHeapSummary;
import one.jfr.event.MallocEvent;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.event.InitialSystemProperty;
import org.bithon.agent.rpc.brpc.profiling.HeapSummary;
import org.bithon.agent.rpc.brpc.profiling.Lock;
import org.bithon.agent.rpc.brpc.profiling.Malloc;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.SystemProperties;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

/**
 * @author frank.chen021@outlook.com
 * @date 4/8/25 11:07 am
 */
public class JfrFileConsumer {
    public interface EventConsumer {
        default void onStart() {}

        /**
         * Called when a JFR event is read.
         *
         * @param event the JFR event
         */
        void onEvent(ProfilingEvent event);

        boolean isCancelled();

        default void onComplete() {}
    }

    public static void consume(File jfrFile, EventConsumer eventConsumer) throws IOException {
        try (JfrFileReader reader = JfrFileReader.createReader(jfrFile.getAbsolutePath())) {
            // Use TreeMap to ensure the system properties are sorted by key
            final Map<String, String> systemProperties = new TreeMap<>();
            eventConsumer.onStart();
            {
                for (Event event; !eventConsumer.isCancelled() && (event = reader.readEvent()) != null; ) {
                    ProfilingEvent profilingEvent = null;
                    if (event instanceof ExecutionSample) {
                        profilingEvent = ProfilingEvent.newBuilder()
                                                       .setCallStackSample(reader.toCallStackSample((ExecutionSample) event))
                                                       .build();

                    } else if (event instanceof CPULoad) {
                        org.bithon.agent.rpc.brpc.profiling.CPULoad cpuLoad = org.bithon.agent.rpc.brpc.profiling.CPULoad.newBuilder()
                                                                                                                         .setTime(reader.toEpochNano(event.time))
                                                                                                                         .setUser(((CPULoad) event).jvmUser)
                                                                                                                         .setSystem(((CPULoad) event).jvmSystem)
                                                                                                                         .setMachine(((CPULoad) event).machineTotal)
                                                                                                                         .build();
                        profilingEvent = ProfilingEvent.newBuilder()
                                                       .setCpuLoad(cpuLoad)
                                                       .build();

                    } else if (event instanceof InitialSystemProperty) {

                        systemProperties.put(((InitialSystemProperty) event).key, ((InitialSystemProperty) event).value);

                    } else if (event instanceof GCHeapSummary) {
                        GCHeapSummary heap = (GCHeapSummary) event;
                        profilingEvent = ProfilingEvent.newBuilder()
                                                       .setHeapSummary(HeapSummary.newBuilder()
                                                                                  .setTime(reader.toEpochNano(event.time))
                                                                                  .setGcId(heap.gcId)
                                                                                  .setUsed(heap.used)
                                                                                  .setCommitted(heap.committed)
                                                                                  .setReserved(heap.reserved)
                                                                                  .build())
                                                       .build();

                    } else if (event instanceof MallocEvent) { // The native memory allocation event
                        MallocEvent mallocEvent = (MallocEvent) event;
                        if (mallocEvent.stackTraceId == 0 || mallocEvent.size == 0) {
                            continue;
                        }

                        Malloc malloc = Malloc.newBuilder()
                                              .setTime(reader.toEpochNano(mallocEvent.time))
                                              .setThreadId(mallocEvent.tid)
                                              .setThreadName(reader.getThreadName(mallocEvent.tid))
                                              .setSize(mallocEvent.size)
                                              .addAllStackTrace(reader.getStackTrace(mallocEvent.stackTraceId))
                                              .build();
                        profilingEvent = ProfilingEvent.newBuilder().setMalloc(malloc).build();

                    } else if (event instanceof AllocationSample) {
                        AllocationSample allocationSample = (AllocationSample) event;
                        if (allocationSample.stackTraceId == 0) {
                            continue;
                        }

                        org.bithon.agent.rpc.brpc.profiling.AllocationSample sample = org.bithon.agent.rpc.brpc.profiling.AllocationSample.newBuilder()
                                                                                                                                          .setTime(reader.toEpochNano(allocationSample.time))
                                                                                                                                          .setThreadId(allocationSample.tid)
                                                                                                                                          .setThreadName(reader.getThreadName(allocationSample.tid))
                                                                                                                                          .addAllStackTrace(reader.getStackTrace(allocationSample.stackTraceId))
                                                                                                                                          .setAllocationSize(allocationSample.allocationSize)
                                                                                                                                          .setTlabSize(allocationSample.tlabSize)
                                                                                                                                          .setClazz(reader.getClassName(allocationSample.classId))
                                                                                                                                          .build();
                        profilingEvent = ProfilingEvent.newBuilder().setAllocationSample(sample).build();

                    } else if (event instanceof ContendedLock) {
                        ContendedLock contendedLock = (ContendedLock) event;
                        if (contendedLock.stackTraceId == 0) {
                            continue;
                        }

                        Lock lock = Lock.newBuilder()
                                        .setTime(reader.toEpochNano(contendedLock.time))
                                        .setThreadId(contendedLock.tid)
                                        .setThreadName(reader.getThreadName(contendedLock.tid))
                                        .addAllStackTrace(reader.getStackTrace(event.stackTraceId))
                                        .setLockClass(reader.getClassName(((ContendedLock) event).classId))
                                        .setWaitTime(((ContendedLock) event).duration)
                                        .build();

                        profilingEvent = ProfilingEvent.newBuilder().setLock(lock).build();
                    }
                    if (profilingEvent != null) {
                        eventConsumer.onEvent(profilingEvent);
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
}
