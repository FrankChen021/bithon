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

package org.bithon.agent.controller.cmd.profiling.asyncprofiler;


import one.profiler.AsyncProfiler;
import org.bithon.agent.controller.cmd.profiling.ProfilingException;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.JfrFileConsumer;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.JfrFileMonitor;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.TimestampedFile;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.StringUtils;

import java.io.File;
import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 22/8/25 4:56 pm
 */
public class AsyncProfilerTask implements Runnable {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AsyncProfilerTask.class);

    private final File outputDir;
    private final int durationSecond;
    private final int intervalSecond;
    private final String profilingEvents;
    private final long endTimestamp;
    private final StreamResponse<ProfilingEvent> streamResponse;
    private final ProgressNotifier progressNotifier;
    private final AsyncProfiler profiler;

    public AsyncProfilerTask(File outputDir,
                             ProfilingRequest profilingRequest,
                             long endTimestamp,
                             StreamResponse<ProfilingEvent> streamResponse) {
        this.outputDir = outputDir;
        this.durationSecond = profilingRequest.getDurationInSeconds();
        this.intervalSecond = profilingRequest.getIntervalInSeconds();
        this.profilingEvents = String.join(",", profilingRequest.getProfileEventsList());
        this.streamResponse = streamResponse;
        this.progressNotifier = new ProgressNotifier(streamResponse);
        this.endTimestamp = endTimestamp;

        this.profiler = AsyncProfiler.getInstance(null);
    }

    @Override
    public void run() {
        this.progressNotifier.sendProgress("Starting profiling");
        startProfiling();

        LOG.info("Started profiling for {} seconds, end at {}", durationSecond, DateTime.formatDateTime("MM-dd HH:mm:ss.SSS", endTimestamp));
        collectProfilingData();

        progressNotifier.sendProgress("Profiling completed");
        streamResponse.onComplete();
    }

    public boolean isTaskCancelled() {
        return this.streamResponse.isCancelled() || this.endTimestamp <= System.currentTimeMillis();
    }

    /**
     * Stop the profiler and clean up resources
     */
    public void stopProfiling() {
        try {
            profiler.stop();
        } catch (IllegalStateException e) {
            // Ignore if profiler wasn't running
        }
    }

    private void startProfiling() {
        try {
            // First ensure profiler is stopped
            try {
                profiler.stop();
            } catch (IllegalStateException e) {
                // Ignore if profiler wasn't running
            }

            // Configure the profiler for JFR output with loop mode
            String jfrOutputPattern = new File(outputDir, "%t.jfr").getAbsolutePath();
            String events = profilingEvents.isEmpty() ? "cpu" : profilingEvents;
            String command = StringUtils.format("start,event=%s,jfr,file=%s,loop=%ds,duration=%s",
                                                events,
                                                jfrOutputPattern,
                                                intervalSecond,
                                                durationSecond);

            profiler.execute(command);
            progressNotifier.sendProgress("Profiler started successfully with events: %s", events);
        } catch (IOException | IllegalStateException e) {
            String errorMsg = "Failed to start profiling: " + e.getMessage();
            throw new ProfilingException(errorMsg, e);
        }
    }

    private void collectProfilingData() {
        JfrFileMonitor monitor = new JfrFileMonitor(outputDir, intervalSecond, this::isTaskCancelled, progressNotifier);

        while (!isTaskCancelled()) {
            TimestampedFile jfrFile = monitor.poll();
            if (jfrFile == null) {
                // No file ready or task cancelled
                break;
            }

            File path = jfrFile.getPath();
            String name = jfrFile.getName();
            long size = jfrFile.getSize();

            try {
                JfrFileConsumer.consume(path,
                                        new JfrFileConsumer.EventConsumer() {
                                            private int eventCount = 0;

                                            @Override
                                            public void onStart() {
                                                progressNotifier.sendProgress("%s is ready and has a size of %s data. Streaming profiling data...",
                                                                              name,
                                                                              HumanReadableNumber.format(size, 2, HumanReadableNumber.UnitSystem.BINARY_BYTE));
                                            }

                                            @Override
                                            public void onEvent(ProfilingEvent event) {
                                                streamResponse.onNext(event);
                                                eventCount++;
                                            }

                                            @Override
                                            public boolean isCancelled() {
                                                return isTaskCancelled();
                                            }

                                            @Override
                                            public void onComplete() {
                                                progressNotifier.sendProgress("%s end of streaming, %s events sent.", name, eventCount);
                                            }
                                        }
                );
            } catch (IOException e) {
                // Only catch IOException for this file to ignore it
                progressNotifier.sendProgress("Ignored profiling data %s: %s", name, e.getMessage());
            } finally {
                if (!path.delete()) {
                    LOG.warn("Failed to delete profiling file: {}", name);
                }
            }
        }
    }
}
