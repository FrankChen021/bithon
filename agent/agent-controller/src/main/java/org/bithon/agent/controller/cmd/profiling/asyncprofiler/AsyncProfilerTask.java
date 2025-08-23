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


import org.bithon.agent.controller.cmd.profiling.ProfilingException;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.JfrFileConsumer;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.TimestampedFile;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.time.DateTime;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 22/8/25 4:56 pm
 */
public class AsyncProfilerTask implements Runnable {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AsyncProfilerTask.class);

    private final String toolLocation;
    private final String pid;
    private final File outputDir;
    private final int durationSecond;
    private final int intervalSecond;
    private final String profilingEvents;
    private final long endTimestamp;
    private final StreamResponse<ProfilingEvent> streamResponse;
    private final ProgressNotifier progressNotifier;

    public AsyncProfilerTask(String toolLocation,
                             String pid,
                             File outputDir,
                             ProfilingRequest profilingRequest,
                             long endTimestamp,
                             StreamResponse<ProfilingEvent> streamResponse) {
        this.toolLocation = toolLocation;
        this.pid = pid;
        this.outputDir = outputDir;
        this.durationSecond = profilingRequest.getDurationInSeconds();
        this.intervalSecond = profilingRequest.getIntervalInSeconds();
        this.profilingEvents = String.join(",", profilingRequest.getProfileEventsList());
        this.streamResponse = streamResponse;
        this.progressNotifier = new ProgressNotifier(streamResponse);
        this.endTimestamp = endTimestamp;
    }

    @Override
    public void run() {
        this.progressNotifier.sendProgress("Starting profiling for PID " + pid);
        startProfiling();

        LOG.info("Started profiling for {} seconds, end at {}", durationSecond, DateTime.formatDateTime("MM-dd HH:mm:ss.SSS", endTimestamp));
        collectProfilingData();

        progressNotifier.sendProgress("Profiling completed for PID %s", pid);
        streamResponse.onComplete();
    }

    public boolean isTaskCancelled() {
        return this.streamResponse.isCancelled() || this.endTimestamp <= System.currentTimeMillis();
    }

    private void startProfiling() {
        ProcessBuilder pb = new ProcessBuilder(toolLocation,
                                               "-d", String.valueOf(durationSecond),
                                               "--loop", intervalSecond + "s",
                                               "-f", new File(outputDir, "%t.jfr").getAbsolutePath(),
                                               // Use the requested events or default to "cpu" if none specified
                                               "-e", profilingEvents.isEmpty() ? "cpu" : profilingEvents,
                                               pid);
        try {
            Process process = pb.start();
            try {
                process.waitFor();
            } catch (InterruptedException ignored) {
            }

            // Read the process output
            try (BufferedReader stderrReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                while (!stderrReader.ready()) {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        break;
                    }
                }

                // Check for success indicators
                String stderrLine = stderrReader.readLine();
                boolean started = (stderrLine != null && stderrLine.contains("started"));
                if (!started) {
                    String errorMsg = StringUtils.format("Failed to start async-profiler - output: %s, alive: %s", stderrLine);
                    throw new ProfilingException(errorMsg);
                }
            }
        } catch (IOException e) {
            String errorMsg = StringUtils.format("Failed to start async-profiler for PID {}: {}", pid, e.getMessage());
            throw new ProfilingException(errorMsg, e);
        }
    }

    private void collectProfilingData() {
        Set<String> skipped = new HashSet<>();

        // Pattern to extract timestamp from filename (format: YYYYMMDD-HHMMSS.jfr)
        Pattern timestampPattern = Pattern.compile("(\\d{8}-\\d{6})\\.jfr");

        PriorityQueue<TimestampedFile> queue = new PriorityQueue<>();
        while (!isTaskCancelled()) {
            File[] files = outputDir.listFiles((file, name) -> name.endsWith(".jfr"));
            if (files != null) {
                for (File file : files) {
                    if (skipped.contains(file.getAbsolutePath())) {
                        continue;
                    }
                    TimestampedFile timestampedFile = TimestampedFile.fromFile(file, timestampPattern);
                    if (timestampedFile != null) {
                        queue.offer(timestampedFile);
                        this.progressNotifier.sendProgress("%s captured, timestamp = %d", timestampedFile.getName(), timestampedFile.getTimestamp());
                    }
                }
            }

            while (!queue.isEmpty() && !this.isTaskCancelled()) {
                TimestampedFile timestampedFile = queue.peek();

                //noinspection DataFlowIssue
                File jfrFilePath = timestampedFile.getPath();
                if (!jfrFilePath.exists()) {
                    queue.poll();

                    // Skip if file doesn't exist anymore
                    continue;
                }

                //
                // Check if enough time has passed since the file's timestamp
                //
                String name = timestampedFile.getName();
                long now = System.currentTimeMillis();
                long fileTimestamp = timestampedFile.getTimestamp();
                long expectedReadyTime = fileTimestamp + this.intervalSecond * 1000L + 500; // Add 500ms buffer
                long waitTime = expectedReadyTime - now;
                while (waitTime > 0 && !this.isTaskCancelled() && !Thread.currentThread().isInterrupted()) {
                    this.progressNotifier.sendProgress("%s is under collection. Waiting...", name);

                    long sleepTime = Math.min(waitTime, 1000); // Sleep in chunks of 1 second
                    waitTime -= sleepTime;
                    try {
                        Thread.sleep(sleepTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Additional check: verify file is complete and stable
                long size = waitForCompleteAndReturnSize(jfrFilePath.toPath());
                if (size < 0) {
                    continue;
                }

                // Pop the file from the queue
                queue.poll();

                try {
                    JfrFileConsumer.consume(jfrFilePath,
                                            new JfrFileConsumer.EventConsumer() {
                                                @Override
                                                public void onStart() {
                                                    progressNotifier.sendProgress("%s is ready and has a size of %s data. Streaming profiling data...",
                                                                                  name,
                                                                                  HumanReadableNumber.format(size, 2, HumanReadableNumber.UnitSystem.BINARY_BYTE));
                                                }

                                                @Override
                                                public void onEvent(ProfilingEvent event) {
                                                    streamResponse.onNext(event);
                                                }

                                                @Override
                                                public boolean isCancelled() {
                                                    return isTaskCancelled();
                                                }

                                                @Override
                                                public void onComplete() {
                                                    progressNotifier.sendProgress("%s end of collection and streaming.", name);
                                                }
                                            }
                    );
                } catch (IOException e) {
                    // Only catch IOException for this file to ignore it
                    progressNotifier.sendProgress("Failed to collect profiling events from %s: %s", name, e.getMessage());
                } finally {
                    if (!jfrFilePath.delete()) {
                        LOG.warn("Failed to delete profiling file: {}", name);
                        skipped.add(jfrFilePath.getAbsolutePath());
                    }
                }
            }
        }
    }

    /**
     * Check if a file is complete by checking if its size is stable
     * This is a simple and reliable approach that works across all platforms
     */
    private long waitForCompleteAndReturnSize(Path filePath) {
        File file = filePath.toFile();

        // First check if file exists and has content
        if (!file.exists() || file.length() == 0) {
            return -1;
        }

        // Check if file size is stable (hasn't changed in the last 500ms)
        long size1 = file.length();
        long lastModified1 = file.lastModified();

        try {
            Thread.sleep(500); // Wait 500ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }

        long size2 = file.length();
        long lastModified2 = file.lastModified();

        // File is complete if size and last modified time haven't changed
        boolean isStable = (size1 == size2) && (lastModified1 == lastModified2) && (size1 > 0);
        if (!isStable) {
            LOG.debug("File {} is still changing: size {} -> {}, lastModified {} -> {}",
                      filePath, size1, size2, lastModified1, lastModified2);
        }

        return isStable ? size1 : -1;
    }
}
