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


import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import lombok.extern.slf4j.Slf4j;
import one.jfr.JfrReader;
import one.jfr.StackTrace;
import one.jfr.event.Event;
import one.jfr.event.ExecutionSample;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.uuid.UUIDv7Generator;
import org.bithon.server.web.service.diagnosis.event.CallStackSample;
import org.bithon.server.web.service.diagnosis.event.SystemProperties;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 21/5/25 6:45 pm
 */
@Slf4j
@CrossOrigin
@RestController
public class JfrApi {

    public JfrApi() {
    }

    @PostMapping("/api/diagnosis/profiling")
    public SseEmitter profiling() {
        SseEmitter emitter = new SseEmitter((30 + 10) * 1000L); // 30 seconds timeout

        String uuid = UUIDv7Generator.create(UUIDv7Generator.INCREMENT_TYPE_DEFAULT)
                                     .generate()
                                     .toCompactFormat();

        // Determine OS and arch
        String os = System.getProperty("os.name").toLowerCase();
        String arch = System.getProperty("os.arch").toLowerCase();
        String profilerDir;
        if (os.contains("mac")) {
            profilerDir = "macos";
        } else if (os.contains("linux")) {
            if (arch.contains("aarch64") || arch.contains("arm64")) {
                profilerDir = "linux-arm64";
            } else {
                profilerDir = "linux-amd64";
            }
        } else {
            throw new RuntimeException("Unsupported OS: " + os);
        }
        String profilerHome = "/Users/frank.chenling/source/open/bithon/agent/agent-distribution/tools/async-profiler-4.0/" + profilerDir + "/bin/asprof";

        // Configuration
        int loopIntervalSeconds = 3; // --loop parameter value
        int totalDurationSeconds = 30; // -d parameter value

        // Example: run asprof to profile current JVM for 30s and output JFR files every 3s
        String pid = String.valueOf(ProcessHandle.current().pid());
        // Create a directory for the UUID if it doesn't exist
        File dir = new File("/tmp/" + uuid);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + dir.getAbsolutePath());
        }
        File uuidFile = new File(dir, "%t.jfr");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (totalDurationSeconds + 5) * 1000L;

        StreamResponse<Event> streamResponse = new StreamResponse<>() {
            @Override
            public void onNext(Event event) {
                try {
                    emitter.send(SseEmitter.event()
                                           .name(event.getClass().getSimpleName())
                                           .data(event.toString(), MediaType.TEXT_PLAIN)
                                           .build());
                } catch (IOException | IllegalStateException ignored) {
                }
            }

            @Override
            public void onException(Throwable throwable) {
                emitter.completeWithError(throwable);
            }

            @Override
            public void onComplete() {
                emitter.complete();
            }
        };

        new Thread(() -> {
            ProcessBuilder pb = new ProcessBuilder(
                profilerHome,
                "-d", String.valueOf(totalDurationSeconds),
                "--loop", loopIntervalSeconds + "s",
                "-f", uuidFile.getAbsolutePath(),
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

                    String stderrLine = stderrReader.readLine();
                    log.info("Async profiler stderr: {}", stderrLine);

                    // Check for success indicators
                    boolean started = (stderrLine != null && stderrLine.contains("started"));
                    if (!started) {
                        String errorMsg = String.format("Failed to start async-profiler - output: %s, alive: %s", stderrLine);
                        log.error(errorMsg);
                        emitter.send(SseEmitter.event()
                                               .data(errorMsg, MediaType.TEXT_PLAIN)
                                               .build());
                        return;
                    }

                    log.info("Async profiler started successfully");
                    emitter.send(SseEmitter.event()
                                           .data("Started", MediaType.TEXT_PLAIN)
                                           .build());
                }
            } catch (IOException e) {
                log.error("Failed to start async-profiler for PID {}: {}", pid, e.getMessage());
                emitter.completeWithError(e);
                return;
            }
            log.info("Started async-profiler asprof for PID {}: {}", pid, profilerHome);
            // TODO: check the process output for errors

            try {
                watchProfilingResult(dir, loopIntervalSeconds, endTime, streamResponse);
            } catch (Exception e) {
                log.error("File processor thread failed", e);
            } finally {
                streamResponse.onComplete();

                log.info("File processor thread completed, deleting directory: {}", dir.getAbsolutePath());
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

            log.info("Stopping profiler for PID {} after {} seconds", pid, totalDurationSeconds);
            // Terminate the profiler process after the duration
            ProcessBuilder pb2 = new ProcessBuilder(profilerHome, "stop", pid);
            try {
                pb2.start();
            } catch (IOException ignored) {
            }
        }).start();

        return emitter;
    }

    /**
     * Process files from the blocking queue based on timestamp ordering and loop interval
     */
    private void watchProfilingResult(File dir,
                                      int loopIntervalSeconds,
                                      long endTime,
                                      StreamResponse<Event> outputStream) {
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
                        log.info("Added existing JFR file to queue: {} with timestamp {}", file.getName(), timestampedFile.getTimestamp());
                    }
                }
            }

            while (!queue.isEmpty()) {
                TimestampedFile timestampedFile = queue.peek();

                File jfrFile = timestampedFile.getFile();
                String jfrFileName = jfrFile.getName();

                // Skip if file doesn't exist anymore
                if (!jfrFile.exists()) {
                    log.debug("Skipping non-existent file: {}", jfrFileName);
                    queue.poll();
                    continue;
                }

                // Check if enough time has passed since the file's timestamp
                long currentTime = System.currentTimeMillis();
                long fileTimestamp = timestampedFile.getTimestamp();
                long readyTime = fileTimestamp + loopIntervalSeconds * 1000L + 500; // Add 500ms buffer
                long waitTime = readyTime - currentTime;

                if (waitTime > 0) {
                    log.info("JFR file {} is not ready yet, waiting {}ms (file timestamp: {}, current: {}, ready at: {})",
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
                        log.info("Processing JFR file from queue: {}", jfrFileName);
                        processJfrFile(jfrFile.toPath(), outputStream);
                    } finally {
                        if (!jfrFile.delete()) {
                            log.warn("Failed to delete JFR file: {}", jfrFileName);
                            skipped.add(jfrFile.getAbsolutePath());
                        }
                    }
                }
            }
        }
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
                    log.warn("Failed to parse timestamp from filename: {}", file.getName(), e);
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
            log.debug("File {} is still changing: size {} -> {}, lastModified {} -> {}",
                      filePath, size1, size2, lastModified1, lastModified2);
        }

        return isStable;
    }

    interface JFRReadEventHandler {
        void onStart();

        /**
         * Called when a JFR event is read.
         *
         * @param jfrEvent the JFR event
         */
        void onEvent(Event jfrEvent);

        void onComplete();
    }

    /**
     * Process a complete JFR file
     */
    private void processJfrFile(Path filePath, StreamResponse<Event> streamResponse) {
        readJFR(filePath,
                (event) -> {
                                /*
                    String eventType = jfrEvent.getEventType().getName();
                    return JdkTypeIDs.CPU_LOAD.equals(eventType)
                           || JdkTypeIDs.EXECUTION_SAMPLE.equals(eventType)
                           || JdkTypeIDs.SYSTEM_PROPERTIES.equals(eventType);
                     */
                    return true;
                },
                new JFRReadEventHandler() {
                    private final Map<String, String> systemProperties = new HashMap<>();

                    @Override
                    public void onStart() {
                    }

                    @Override
                    public void onEvent(Event jfrEvent) {
                        if (jfrEvent instanceof InitialSystemProperty) {
                            systemProperties.put(
                                ((InitialSystemProperty) jfrEvent).key,
                                ((InitialSystemProperty) jfrEvent).value
                            );
                        } else {
                            streamResponse.onNext(jfrEvent);
                        }
                    }

                    @Override
                    public void onComplete() {
                        if (!systemProperties.isEmpty()) {
                            streamResponse.onNext(new SystemProperties(systemProperties));
                        }
                    }
                }
        );
    }

    private void readJFR(Path file,
                         Predicate<Event> filter,
                         JFRReadEventHandler eventHandler
    ) {
        try (JfrReader jfr = createJfrReader(file.toFile().getAbsolutePath())) {
            eventHandler.onStart();
            {
                for (Event event; (event = jfr.readEvent()) != null;) {
                    if (event instanceof ExecutionSample) {
                        event = CallStackSample.toCallStackSample(jfr, (ExecutionSample) event);
                        if (event == null) {
                            continue;
                        }
                    }

                    if (filter != null && !filter.test(event)) {
                        continue;
                    }

                    eventHandler.onEvent(event);
                }
            }
            eventHandler.onComplete();
        } catch (IOException e) {
            log.error("Failed to read JFR file: {}", file, e);
        }
        /*
        try (RecordingFile rf = new RecordingFile(file)) {
            onStart.run();
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                if (filter != null && !filter.test(event)) {
                    continue; // Skip events that do not match the filter
                }
                eventConsumer.accept(event);
            }
            onComplete.run();
        } catch (IOException e) {
            log.error("Failed to read JFR file: {}", file, e);
        }*/
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

    public static void main(String[] args) {
        File f = new File("/Users/frank.chenling/source/open/bithon/agent/agent-distribution/tools/async-profiler-4.0/macos/bin/20250712-224458.jfr");
        try (JfrReader jfrReader = createJfrReader(f.getAbsolutePath())) {
            {
                Event event = jfrReader.readEvent();
                while (event != null) {
                    long time = jfrReader.startNanos + ((event.time - jfrReader.startTicks) / jfrReader.ticksPerSec);
                    System.out.printf("%d, %s\n", time, event);
                    StackTrace stackTrace = jfrReader.stackTraces.get(event.stackTraceId);
                    if (stackTrace != null) {
                        org.bithon.server.web.service.diagnosis.StackTrace st = CallStackSample.toStackTrace(jfrReader, event.tid, stackTrace);
                        System.out.println(st);
                    }
                    event = jfrReader.readEvent();
                }
            }
        } catch (IOException ignored) {
        }
        System.out.println("=========================================");
        try (RecordingFile rf = new RecordingFile(f.toPath())) {
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();

                System.out.println(event);
            }
        } catch (IOException ignored) {
        }
    }
}
