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

package org.bithon.server.web.service.diagnosis;


import one.jfr.JfrReader;
import one.jfr.event.CPULoad;
import one.jfr.event.Event;
import one.jfr.event.ExecutionSample;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.component.commons.uuid.UUIDv7Generator;
import org.bithon.server.web.service.diagnosis.event.CPUUsage;
import org.bithon.server.web.service.diagnosis.event.CallStackSample;
import org.bithon.server.web.service.diagnosis.event.IEvent;
import org.bithon.server.web.service.diagnosis.event.SystemProperties;
import org.bithon.server.web.service.diagnosis.event.TimeConverter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/25 8:11 pm
 */
public class Profiler {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(Profiler.class);

    public static final Profiler INSTANCE = new Profiler();

    public void start(int intervalSeconds, int durationSeconds, StreamResponse<IEvent> streamResponse) {
        if (intervalSeconds <= 0 || durationSeconds <= 0) {
            streamResponse.onException(new IllegalArgumentException("Interval and duration must be greater than 0"));
            return;
        }

        String uuid = UUIDv7Generator.create(UUIDv7Generator.INCREMENT_TYPE_DEFAULT)
                                     .generate()
                                     .toCompactFormat();

        // Determine OS and arch
        String profilerHome = getToolLocation();

        // Configuration
        int loopIntervalSeconds = 3; // --loop parameter value
        int totalDurationSeconds = 30; // -d parameter value

        String pid = String.valueOf(ProcessHandle.current().pid());

        File dir = new File(System.getProperty("java.io.tmpdir", "/tmp"), uuid);
        if (!dir.exists() && !dir.mkdirs()) {
            streamResponse.onException(new RuntimeException("Failed to create directory: " + dir.getAbsolutePath()));
        }

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (totalDurationSeconds + 5) * 1000L;

        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(
                profilerHome,
                "-d", String.valueOf(totalDurationSeconds),
                "--loop", loopIntervalSeconds + "s",
                "-f", new File(dir, "%t.jfr").getAbsolutePath(),
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
                try (BufferedReader stderrReader = process.errorReader()) {
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
                        LOG.error(errorMsg);
                        streamResponse.onException(new RuntimeException(errorMsg));
                        return;
                    }

                    LOG.info("Async profiler started successfully");
                }
            } catch (IOException e) {
                LOG.error("Failed to start async-profiler for PID {}: {}", pid, e.getMessage());
                streamResponse.onException(e);
                return;
            }
            LOG.info("Started async-profiler asprof for PID {}: {}", pid, profilerHome);

            try {
                watchProfilingResult(dir, loopIntervalSeconds, endTime, streamResponse);
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

            LOG.info("Stopping profiler for PID {} after {} seconds", pid, totalDurationSeconds);
            // Terminate the profiler process after the duration
            ProcessBuilder pb2 = new ProcessBuilder(profilerHome, "stop", pid);
            try {
                pb2.start();
            } catch (IOException ignored) {
            }
        }).start();
    }

    private void watchProfilingResult(File dir,
                                      int loopIntervalSeconds,
                                      long endTime,
                                      StreamResponse<IEvent> outputStream) {
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
                        processJfrFile(jfrFile.toPath(), outputStream);
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

    interface FileEventConsumer {
        void onStart();

        /**
         * Called when a JFR event is read.
         *
         * @param jfrEvent the JFR event
         */
        void onEvent(IEvent jfrEvent);

        void onComplete();
    }

    /**
     * Process a complete JFR file
     */
    private void processJfrFile(Path filePath, StreamResponse<IEvent> streamResponse) {
        readJFR(filePath,
                new FileEventConsumer() {
                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onEvent(IEvent jfrEvent) {
                        streamResponse.onNext(jfrEvent);
                    }

                    @Override
                    public void onComplete() {
                    }
                }
        );
    }

    private void readJFR(Path file,
                         FileEventConsumer eventConsumer) {
        try (JfrReader jfr = createJfrReader(file.toFile().getAbsolutePath())) {
            final Map<String, String> systemProperties = new HashMap<>();
            eventConsumer.onStart();
            {
                for (Event event; (event = jfr.readEvent()) != null; ) {
                    IEvent e = null;
                    if (event instanceof ExecutionSample) {
                        e = CallStackSample.toCallStackSample(jfr, (ExecutionSample) event);
                    } else if (event instanceof CPULoad) {
                        e = new CPUUsage(TimeConverter.toEpochNano(jfr, event.time),
                                         ((CPULoad) event).jvmUser,
                                         ((CPULoad) event).jvmSystem,
                                         ((CPULoad) event).machineTotal);
                    } else if (event instanceof InitialSystemProperty) {
                        systemProperties.put(((InitialSystemProperty) event).key,
                                             ((InitialSystemProperty) event).value);
                    }

                                /*
                    String eventType = jfrEvent.getEventType().getName();
                    return JdkTypeIDs.CPU_LOAD.equals(eventType)
                           || JdkTypeIDs.EXECUTION_SAMPLE.equals(eventType)
                           || JdkTypeIDs.SYSTEM_PROPERTIES.equals(eventType);
                     */
                    if (e != null) {
                        eventConsumer.onEvent(e);
                    }
                }
            }
            if (!systemProperties.isEmpty()) {
                eventConsumer.onEvent(new SystemProperties(systemProperties));
            }
        } catch (IOException e) {
            LOG.error("Failed to read JFR file: {}", file, e);
        } finally {
            try {
                eventConsumer.onComplete();
            } catch (Exception ignored) {
            }
        }
    }

    public static JfrReader createJfrReader(String filePath) throws IOException {
        JfrReader jfrReader = new JfrReader(filePath);
        jfrReader.registerEvent("jdk.CPUInformation", CPUInformation.class);
        jfrReader.registerEvent("jdk.InitialSystemProperty", InitialSystemProperty.class);
        jfrReader.registerEvent("jdk.InitialEnvironmentVariable", InitialEnvironmentVariable.class);
        jfrReader.registerEvent("jdk.OSInformation", OSInformation.class);
        jfrReader.registerEvent("jdk.JVMInformation", JVMInformation.class);
        return jfrReader;
    }

    /**
     * Represents a JFR file with its extracted timestamp
     */
    private static class TimestampedFile implements Comparable<TimestampedFile> {
        private final File file;
        private final long timestamp;

        private TimestampedFile(File file, long timestamp) {
            this.file = file;
            this.timestamp = timestamp;
        }

        public static TimestampedFile fromFile(File file, Pattern timestampPattern) {
            Matcher matcher = timestampPattern.matcher(file.getName());
            if (matcher.find()) {
                String timestampStr = matcher.group(1);
                try {
                    // Parse timestamp from format YYYYMMDD-HHMMSS
                    long timestamp = parseTimestamp(timestampStr);
                    return new TimestampedFile(file, timestamp);
                } catch (Exception e) {
                    LOG.warn("Failed to parse timestamp from filename: {}", file.getName(), e);
                }
            }
            return null;
        }

        private static long parseTimestamp(String timestampStr) {
            // Parse YYYYMMDD-HHMMSS format
            String dateStr = timestampStr.substring(0, 8); // YYYYMMDD
            String timeStr = timestampStr.substring(9, 15); // HHMMSS

            int year = Integer.parseInt(dateStr.substring(0, 4));
            int month = Integer.parseInt(dateStr.substring(4, 6));
            int day = Integer.parseInt(dateStr.substring(6, 8));
            int hour = Integer.parseInt(timeStr.substring(0, 2));
            int minute = Integer.parseInt(timeStr.substring(2, 4));
            int second = Integer.parseInt(timeStr.substring(4, 6));

            // Convert to milliseconds since epoch
            java.time.LocalDateTime dateTime = java.time.LocalDateTime.of(year, month, day, hour, minute, second);
            return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
        }

        public File getFile() {
            return file;
        }

        public long getTimestamp() {
            return timestamp;
        }

        @Override
        public String toString() {
            return file.getPath();
        }

        @Override
        public int compareTo(TimestampedFile that) {
            return Long.compare(this.timestamp, that.timestamp);
        }
    }

    private static String getToolLocation() {
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();

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
        return "/Users/frank.chenling/source/open/bithon/agent/agent-distribution/tools/async-profiler-4.0/" + dir + "/bin/asprof";
    }
}
