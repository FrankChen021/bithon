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
import org.bithon.component.commons.uuid.UUIDv7Generator;

import java.io.File;
import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 22/8/25 4:56 pm
 */
public class AsyncProfilerTask implements Runnable {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AsyncProfilerTask.class);

    private File outputDir;
    private final int durationSecond;
    private final int intervalSecond;
    private final String profilingEvents;
    private final long endTimestamp;
    private final StreamResponse<ProfilingEvent> streamResponse;
    private final ProgressNotifier progressNotifier;
    private final AsyncProfiler profiler;
    private final String commandLineArgs;
    private volatile boolean isUserStopped;

    public AsyncProfilerTask(ProfilingRequest profilingRequest,
                             StreamResponse<ProfilingEvent> streamResponse) {
        this.durationSecond = profilingRequest.getDurationInSeconds();
        this.intervalSecond = profilingRequest.getIntervalInSeconds();
        this.commandLineArgs = profilingRequest.getArgs();
        this.profilingEvents = String.join(",", profilingRequest.getProfileEventsList());
        this.streamResponse = streamResponse;
        this.progressNotifier = new ProgressNotifier(streamResponse);

        // Calculate endTime based on duration and interval
        long startTime = System.currentTimeMillis();
        this.endTimestamp = startTime + (durationSecond + intervalSecond + 3) * 1000L;

        this.profiler = AsyncProfiler.getInstance(null);
    }

    @Override
    public void run() {
        // Prepare output directory
        String uuid = UUIDv7Generator.create(UUIDv7Generator.INCREMENT_TYPE_DEFAULT)
                                     .generate()
                                     .toCompactFormat();
        this.outputDir = new File(System.getProperty("java.io.tmpdir", "/tmp"), "org.bithon.agent/profiling/" + uuid);
        if (!outputDir.exists() && !outputDir.mkdirs()) {
            throw new ProfilingException("Failed to create temporary directory: " + outputDir.getAbsolutePath());
        }

        this.progressNotifier.sendProgress("Starting profiling at temporary directory: %s", outputDir.getAbsolutePath());
        startProfiling();

        LOG.info("Started profiling for {} seconds, end at {}", durationSecond, DateTime.formatDateTime("MM-dd HH:mm:ss.SSS", endTimestamp));
        collectProfilingData();

        progressNotifier.sendProgress("Profiling completed");
        streamResponse.onComplete();
    }

    private boolean isTaskCancelled() {
        return isUserStopped || this.streamResponse.isCancelled() || this.endTimestamp <= System.currentTimeMillis();
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

        try {
            if (this.outputDir != null && this.outputDir.exists() && outputDir.isDirectory()) {
                File[] files = outputDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
                outputDir.delete();
            }
        } catch (Exception ignored) {
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
            String command = StringUtils.format("start,event=%s,jfr,file=%s,loop=%ds",
                                                events,
                                                jfrOutputPattern,
                                                intervalSecond);
            if (StringUtils.hasText(commandLineArgs)) {
                command += ("," + commandLineArgs);
            }

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

            File filePath = jfrFile.getPath();
            String name = jfrFile.getName();
            long size = jfrFile.getSize();

            try {
                JfrFileConsumer.consume(filePath,
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
                if (!filePath.delete()) {
                    LOG.warn("Failed to delete profiling file: {}", name);
                }
            }
        }
    }

    /**
     * A stop request
     */
    public void stop() {
        this.progressNotifier.sendProgress("Received stop request from user.");
        isUserStopped = true;
    }
}
