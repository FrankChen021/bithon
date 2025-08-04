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

package org.bithon.agent.controller.cmd.profiling;


import org.bithon.agent.controller.cmd.profiling.jfr.JfrEventConsumer;
import org.bithon.agent.controller.cmd.profiling.jfr.JfrFileReader;
import org.bithon.agent.controller.cmd.profiling.jfr.TimestampedFile;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.component.commons.uuid.UUIDv7Generator;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/25 8:11 pm
 */
public class Profiler {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(Profiler.class);

    public static final Profiler INSTANCE = new Profiler();

    static class ProfilingException extends RuntimeException {
        ProfilingException(String message) {
            super(message);
        }

        ProfilingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    /**
     * called by {@link org.bithon.agent.controller.cmd.ProfilingCommand} by reflection
     */
    public static void start(ProfilingRequest request, StreamResponse<ProfilingEvent> streamResponse) {
        INSTANCE.start(request.getIntervalInSeconds(),
                       request.getDurationInSeconds(),
                       streamResponse);
    }

    public void start(int intervalSeconds, int durationSeconds, StreamResponse<ProfilingEvent> streamResponse) {
        if (intervalSeconds <= 0 || durationSeconds <= 0) {
            streamResponse.onException(new IllegalArgumentException("Interval and duration must be greater than 0"));
            return;
        }

        String uuid = UUIDv7Generator.create(UUIDv7Generator.INCREMENT_TYPE_DEFAULT)
                                     .generate()
                                     .toCompactFormat();

        // Determine OS and arch
        String toolLocation = getToolLocation();
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        File dir = new File(System.getProperty("java.io.tmpdir", "/tmp"), uuid);
        if (!dir.exists() && !dir.mkdirs()) {
            streamResponse.onException(new RuntimeException("Failed to create temporary directory: " + dir.getAbsolutePath()));
            return;
        }

        // Configuration
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds + 5) * 1000L;

        new Thread(() -> {
            tryStopProfiling(toolLocation, pid);

            try {
                startProfiling(toolLocation, pid, durationSeconds, intervalSeconds, dir);
            } catch (ProfilingException e) {
                streamResponse.onException(e);
                return;
            }

            try {
                collectProfilingData(dir, intervalSeconds, endTime, streamResponse);
            } catch (Exception e) {
                LOG.error("File processor thread failed", e);
            } finally {
                streamResponse.onComplete();

                LOG.info("File processor thread completed, deleting directory: {}", dir.getAbsolutePath());
                try {
                    if (dir.exists() && dir.isDirectory()) {
                        File[] files = dir.listFiles();
                        if (files != null) {
                            for (File file : files) {
                                file.delete();
                            }
                        }
                        dir.delete();
                    }
                } catch (Exception ignored) {
                }
            }

            LOG.info("Stopping profiler for PID {} after {} seconds", pid, durationSeconds);

            // Terminate the profiler process after the duration
            tryStopProfiling(toolLocation, pid);
        }).start();
    }

    private void tryStopProfiling(String toolLocation, String pid) {
        try {
            Process process = new ProcessBuilder(toolLocation, "stop", pid).start();
            try {
                process.waitFor();
            } catch (InterruptedException ignored) {
            }
        } catch (IOException ignored) {
        }
    }

    private void startProfiling(String toolLocation, String pid, long durationSecond, long intervalSecond, File outputDir) {
        ProcessBuilder pb = new ProcessBuilder(
            toolLocation,
            "-d", String.valueOf(durationSecond),
            "--loop", intervalSecond + "s",
            "-f", new File(outputDir, "%t.jfr").getAbsolutePath(),
            "-e", "cpu",
            pid
        );
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
        LOG.info("Started profiling for {} seconds", durationSecond);
    }

    private void collectProfilingData(File dir,
                                      int loopIntervalSeconds,
                                      long endTime,
                                      StreamResponse<ProfilingEvent> outputStream) {
        Set<String> skipped = new HashSet<>();

        // Pattern to extract timestamp from filename (format: YYYYMMDD-HHMMSS.jfr)
        Pattern timestampPattern = Pattern.compile("(\\d{8}-\\d{6})\\.jfr");

        PriorityQueue<TimestampedFile> queue = new PriorityQueue<>();
        while (System.currentTimeMillis() < endTime) {
            File[] files = dir.listFiles((file, name) -> name.endsWith(".jfr"));
            if (files != null) {
                for (File file : files) {
                    if (skipped.contains(file.getAbsolutePath())) {
                        continue;
                    }
                    TimestampedFile timestampedFile = TimestampedFile.fromFile(file, timestampPattern);
                    if (timestampedFile != null) {
                        queue.offer(timestampedFile);
                        LOG.info("Added existing JFR file to queue: {} with timestamp {}", file.getName(), timestampedFile.getTimestamp());
                    }
                }
            }

            while (!queue.isEmpty()) {
                TimestampedFile timestampedFile = queue.peek();

                File jfrFile = timestampedFile.getFile();
                String jfrFileName = jfrFile.getName();

                // Skip if file doesn't exist anymore
                if (!jfrFile.exists()) {
                    LOG.debug("Skipping non-existent file: {}", jfrFileName);
                    queue.poll();
                    continue;
                }

                // Check if enough time has passed since the file's timestamp
                long currentTime = System.currentTimeMillis();
                long fileTimestamp = timestampedFile.getTimestamp();
                long readyTime = fileTimestamp + loopIntervalSeconds * 1000L + 500; // Add 500ms buffer
                long waitTime = readyTime - currentTime;

                if (waitTime > 0) {
                    LOG.info("JFR file {} is not ready yet, waiting {}ms (file timestamp: {}, current: {}, ready at: {})",
                             jfrFileName, waitTime, fileTimestamp, currentTime, readyTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Additional check: verify file is complete and stable
                if (waitForComplete(jfrFile.toPath())) {
                    // Pop the file from the queue
                    queue.poll();

                    // File is ready - process it
                    try {
                        LOG.info("Processing JFR file from queue: {}", jfrFileName);
                        JfrFileReader.read(jfrFile,
                                           new JfrEventConsumer() {
                                               @Override
                                               public void onStart() {
                                               }

                                               @Override
                                               public void onEvent(ProfilingEvent event) {
                                                   outputStream.onNext(event);
                                               }

                                               @Override
                                               public void onComplete() {
                                               }
                                           }
                        );
                    } catch (IOException e) {
                        LOG.error("Failed to read JFR file {}: {}", jfrFileName, e.getMessage());
                    } finally {
                        if (!jfrFile.delete()) {
                            LOG.warn("Failed to delete JFR file: {}", jfrFileName);
                            skipped.add(jfrFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
    }

    /**
     * Check if a file is complete by checking if its size is stable
     * This is a simple and reliable approach that works across all platforms
     */
    private boolean waitForComplete(Path filePath) {
        File file = filePath.toFile();

        // First check if file exists and has content
        if (!file.exists() || file.length() == 0) {
            return false;
        }

        // Check if file size is stable (hasn't changed in the last 500ms)
        long size1 = file.length();
        long lastModified1 = file.lastModified();

        try {
            Thread.sleep(500); // Wait 500ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        long size2 = file.length();
        long lastModified2 = file.lastModified();

        // File is complete if size and last modified time haven't changed
        boolean isStable = (size1 == size2) && (lastModified1 == lastModified2) && (size1 > 0);

        if (!isStable) {
            LOG.debug("File {} is still changing: size {} -> {}, lastModified {} -> {}",
                      filePath, size1, size2, lastModified1, lastModified2);
        }

        return isStable;
    }

    private static String getToolLocation() {
        String os = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        String arch = System.getProperty("os.arch").toLowerCase(Locale.ENGLISH);

        String dir;
        if (os.contains("mac")) {
            dir = "macos";
        } else if (os.contains("linux")) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                dir = "linux-arm64";
            } else {
                dir = "linux-amd64";
            }
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        File toolHome = AgentDirectory.getSubDirectory("tools/async-profiler");
        if (!toolHome.exists()) {
            throw new RuntimeException("Async profiler tool directory does not exist: " + toolHome.getAbsolutePath());
        }
        File osDir = new File(toolHome, dir);
        if (!osDir.exists()) {
            throw new RuntimeException("Async profiler tool directory for " + dir + " does not exist: " + osDir.getAbsolutePath());
        }
        return new File(osDir, "bin/asprof").getAbsolutePath();
    }
}
