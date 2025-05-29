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


import jdk.jfr.ValueDescriptor;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import one.jfr.JfrReader;
import one.jfr.event.Event;
import org.bithon.component.commons.uuid.UUIDv7Generator;
import org.openjdk.jmc.common.item.IAttribute;
import org.openjdk.jmc.common.item.IItem;
import org.openjdk.jmc.common.item.IItemCollection;
import org.openjdk.jmc.common.item.IItemIterable;
import org.openjdk.jmc.common.item.IMemberAccessor;
import org.openjdk.jmc.common.item.IType;
import org.openjdk.jmc.common.unit.IQuantity;
import org.openjdk.jmc.flightrecorder.JfrAttributes;
import org.openjdk.jmc.flightrecorder.JfrLoaderToolkit;
import org.openjdk.jmc.flightrecorder.jdk.JdkAttributes;
import org.openjdk.jmc.flightrecorder.jdk.JdkTypeIDs;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
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

    public static void main(String[] args) throws IOException {

        //Path file = Paths.get("/Users/frank.chenling/source/open/bithon/agent/agent-distribution/tools/async-profiler-4.0/macos/bin/output.jfr");
        Path file = Paths.get("/tmp/01970d60a3d4fa3dd68ceebf8bc0c88e/20250527-001552.jfr");
        try (RecordingFile rf = new RecordingFile(file)) {

            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                /*
                if (!event.getEventType().getName().equals("jdk.CPULoad")) {
                    continue;
                }*/
                // dump the event type of event
                System.out.print(event.getEventType().getName());
                System.out.print(" Start Time: " + event.getStartTime());
                System.out.print(" End Time: " + event.getEndTime());
                // dump the event fields
                for (ValueDescriptor fieldName : event.getEventType().getFields()) {
                    System.out.print(" field(" + fieldName.getTypeName() + ")=" + fieldName.getName());
                }
                System.out.println(event.getStackTrace() == null ? " EMPTY" : " HAS STACK TRACE");
            }
        }
    }

    public JfrApi() {
    }

    public Map<String, Object> analyzeJfrFile(InputStream inputStream) {
        try {
            IItemCollection events = JfrLoaderToolkit.loadEvents(inputStream);
            Map<String, Object> rootNode = new LinkedHashMap<>();
            rootNode.put("metadata", createMetadata(events));
            rootNode.put("metrics", createMetrics(events));
            return rootNode;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> analyzeJfrFile(File jfrFile) throws IOException {
        if (!jfrFile.exists()) {
            throw new RuntimeException("File not found: " + jfrFile.getAbsolutePath());
        }
        try (InputStream is = new FileInputStream(jfrFile)) {
            return analyzeJfrFile(is);
        }
    }

    @PostMapping("/api/diagnosis/jfr")
    public Map<String, Object> analyzeJfrUpload(@RequestParam("file") MultipartFile file) throws IOException {
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

        return analyzeJfrFile(file.getInputStream());
    }

    @PostMapping("/api/diagnosis/profiling")
    public void profiling() throws IOException {
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
        ProcessBuilder pb = new ProcessBuilder(
            profilerHome,
            "-d", String.valueOf(totalDurationSeconds),
            "--loop", loopIntervalSeconds + "s",
            "-f", uuidFile.getAbsolutePath(),
            "-e", "cpu",
            pid
        );
        pb.inheritIO(); // or redirect output as needed
        Process process = pb.start();
        log.info("Started async-profiler asprof for PID {}: {}", pid, profilerHome);

        // Optionally, you can wait for the process to finish in a background thread
        new Thread(() -> {
            try {
                process.waitFor();
                log.info("Profiler finished for PID {}: {}", pid, profilerHome);
            } catch (Exception ignored) {
            }
        }).start();

        // Blocking queue to communicate between watcher and processor threads
        BlockingQueue<TimestampedFile> fileQueue = new LinkedBlockingQueue<>();
        
        // Pattern to extract timestamp from filename (format: YYYYMMDD-HHMMSS.jfr)
        Pattern timestampPattern = Pattern.compile("(\\d{8}-\\d{6})\\.jfr");

        long startTime = System.currentTimeMillis();
        long endTime = startTime + (totalDurationSeconds + 5) * 1000L;

        // Start file watcher thread
        Thread watcherThread = new Thread(() -> {
            try {
                watchFiles(dir, fileQueue, timestampPattern, endTime);
            } catch (Exception e) {
                log.error("File watcher thread failed", e);
            }
        }, "JFR-FileWatcher");
        watcherThread.start();

        // Start file processor thread
        Thread processorThread = new Thread(() -> {
            try {
                processTimestampedFiles(fileQueue, loopIntervalSeconds, endTime);
            } catch (Exception e) {
                log.error("File processor thread failed", e);
            }
        }, "JFR-FileProcessor");
        processorThread.start();

        // Wait for both threads to complete
        try {
            watcherThread.join();
            processorThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Profiling interrupted", e);
        }
    }

    /**
     * Watch for new JFR files and add them to the queue
     */
    private void watchFiles(File dir, BlockingQueue<TimestampedFile> fileQueue, Pattern timestampPattern, long endTime) throws IOException {
        WatchService watchService = FileSystems.getDefault().newWatchService();
        dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE);

        // First, add any existing JFR files to the queue (in case some were created before we started watching)
        File[] existingFiles = dir.listFiles((file, name) -> name.endsWith(".jfr"));
        if (existingFiles != null) {
            for (File file : existingFiles) {
                TimestampedFile timestampedFile = TimestampedFile.fromFile(file, timestampPattern);
                if (timestampedFile != null) {
                    try {
                        fileQueue.put(timestampedFile);
                        log.debug("Added existing JFR file to queue: {} with timestamp {}", file.getName(), timestampedFile.getTimestamp());
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        while (System.currentTimeMillis() < endTime) {
            try {
                // Wait for file system events (with timeout)
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);

                if (key != null) {
                    // Process file system events
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        String fileName = ev.context().toString();

                        if (fileName.endsWith(".jfr") && timestampPattern.matcher(fileName).matches()) {
                            // Add new JFR file to queue
                            File newFile = new File(dir, fileName);
                            TimestampedFile timestampedFile = TimestampedFile.fromFile(newFile, timestampPattern);
                            if (timestampedFile != null) {
                                try {
                                    fileQueue.put(timestampedFile);
                                    log.info("New JFR file detected and added to queue: {} with timestamp {}", 
                                            fileName, timestampedFile.getTimestamp());
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                    break;
                                }
                            }
                        }
                    }

                    if (!key.reset()) {
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        try {
            watchService.close();
        } catch (IOException e) {
            log.warn("Failed to close watch service", e);
        }
        
        log.info("File watcher thread completed");
    }

    /**
     * Process files from the blocking queue based on timestamp ordering and loop interval
     */
    private void processTimestampedFiles(BlockingQueue<TimestampedFile> fileQueue, 
                                       int loopIntervalSeconds, 
                                       long endTime) {
        while (System.currentTimeMillis() < endTime) {
            try {
                // Wait for a file to be available in the queue (with timeout)
                TimestampedFile timestampedFile = fileQueue.poll(1, TimeUnit.SECONDS);
                if (timestampedFile == null) {
                    continue; // Timeout, check if we should continue
                }

                File jfrFile = timestampedFile.getFile();
                String jfrFileName = jfrFile.getName();

                // Skip if file doesn't exist anymore
                if (!jfrFile.exists()) {
                    log.debug("Skipping non-existent file: {}", jfrFileName);
                    continue;
                }

                // Check if enough time has passed since the file's timestamp
                long currentTime = System.currentTimeMillis();
                long fileTimestamp = timestampedFile.getTimestamp();
                long readyTime = fileTimestamp + loopIntervalSeconds * 1000L + 500; // Add 500ms buffer
                long waitTime = readyTime - currentTime;
                
                if (waitTime > 0) {
                    log.debug("JFR file {} is not ready yet, waiting {}ms (file timestamp: {}, current: {}, ready at: {})", 
                             jfrFileName, waitTime, fileTimestamp, currentTime, readyTime);
                    try {
                        Thread.sleep(waitTime);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }

                // Additional check: verify file is complete and stable
                if (isFileComplete(jfrFile.toPath())) {
                    // File is ready - process it
                    log.info("Processing JFR file from queue: {}", jfrFileName);
                    processJfrFile(jfrFile.toPath());
                    log.debug("Successfully processed file: {}", jfrFileName);
                } else {
                    // File exists and time has passed, but file is still being written
                    log.debug("JFR file {} is ready by time but still being written, putting back in queue...", jfrFileName);
                    try {
                        fileQueue.put(timestampedFile); // Put it back for later processing
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        log.info("File processor thread completed");
    }

    /**
     * Represents a JFR file with its extracted timestamp
     */
    private static class TimestampedFile {
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
    }

    /**
     * Check if a file is complete by checking if its size is stable
     * This is a simple and reliable approach that works across all platforms
     */
    private boolean isFileComplete(Path filePath) {
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

    /**
     * Process a complete JFR file
     */
    private void processJfrFile(Path filePath) {
        readJFR(filePath,
                (jfrEvent) -> {
                    // Filter events if needed, e.g., only CPU load events
                    String eventType = jfrEvent.getEventType().getName();
                    return JdkTypeIDs.CPU_LOAD.equals(eventType)
                           || JdkTypeIDs.EXECUTION_SAMPLE.equals(eventType)
                           || JdkTypeIDs.SYSTEM_PROPERTIES.equals(eventType);
                },
                (jfrEvent) -> {
                    // Process each event as needed
                    System.out.println("Event: " + jfrEvent.getEventType().getName() + " at " + jfrEvent.getStartTime());
                });
    }

    private void readJFR(Path file,
                         Predicate<RecordedEvent> filter,
                         Consumer<RecordedEvent> eventConsumer) {
        try (RecordingFile rf = new RecordingFile(file)) {
            while (rf.hasMoreEvents()) {
                RecordedEvent event = rf.readEvent();
                if (filter != null && !filter.test(event)) {
                    continue; // Skip events that do not match the filter
                }
                eventConsumer.accept(event);
            }
        } catch (IOException e) {
            log.error("Failed to read JFR file: {}", file, e);
        }
    }

    private Map<String, Object> createMetadata(IItemCollection events) {
        Map<String, Object> metadataNode = new LinkedHashMap<>();

        // Extract recording start and end time
        long startTime = Long.MAX_VALUE;
        long endTime = Long.MIN_VALUE;

        for (IItemIterable itemIterable : events) {
            IType<IItem> type = itemIterable.getType();
            IMemberAccessor<IQuantity, IItem> startTimeAccessor = JfrAttributes.START_TIME.getAccessor(type);
            IMemberAccessor<IQuantity, IItem> endTimeAccessor = JfrAttributes.END_TIME.getAccessor(type);

            if (startTimeAccessor != null && endTimeAccessor != null) {
                for (IItem item : itemIterable) {
                    IQuantity startTimeQuantity = startTimeAccessor.getMember(item);
                    IQuantity endTimeQuantity = endTimeAccessor.getMember(item);

                    if (startTimeQuantity != null) {
                        startTime = Math.min(startTime, startTimeQuantity.longValue());
                    }

                    if (endTimeQuantity != null) {
                        endTime = Math.max(endTime, endTimeQuantity.longValue());
                    }
                }
            }
        }

        // If no events found, use current time
        if (startTime == Long.MAX_VALUE) {
            startTime = System.currentTimeMillis();
        }

        if (endTime == Long.MIN_VALUE) {
            endTime = System.currentTimeMillis();
        }

        metadataNode.put("startTime", startTime);
        metadataNode.put("endTime", endTime);
        metadataNode.put("duration", endTime - startTime);
        metadataNode.put("version", "2.0"); // Default JFR version

        return metadataNode;
    }

    @Getter
    static class Metric {
        final long timestamp;
        final double value;

        public Metric(double value, long timestamp) {
            this.value = value;
            this.timestamp = timestamp;
        }
    }

    static class Accessor {
        private final String name;
        private final IMemberAccessor<IQuantity, IItem> impl;

        private Accessor(IAttribute<IQuantity> attribute, IType<IItem> type) {
            this.name = attribute.getIdentifier();
            this.impl = attribute.getAccessor(type);
        }

        static Accessor of(IAttribute<IQuantity> attribute, IType<IItem> type) {
            return new Accessor(attribute, type);
        }
    }

    private Map<String, Object> createMetrics(IItemCollection jfrEvents) {
        Map<String, Object> metrics = new HashMap<>();
        for (IItemIterable jfrEvent : jfrEvents) {
            String typeId = jfrEvent.getType().getIdentifier();
            List<Accessor> accessors = new ArrayList<>();
            // CPU
            if (JdkTypeIDs.CPU_LOAD.equals(typeId)) {
                accessors.add(Accessor.of(JdkAttributes.JVM_USER, jfrEvent.getType()));
                accessors.add(Accessor.of(JdkAttributes.JVM_SYSTEM, jfrEvent.getType()));
            }
            // Memory
            else if (JdkTypeIDs.HEAP_SUMMARY.equals(typeId)) {
                accessors.add(Accessor.of(JdkAttributes.HEAP_USED, jfrEvent.getType()));
            } else {
                System.out.println("Unknown type: " + typeId);
                continue;
            }

            IMemberAccessor<IQuantity, IItem> timeAccessor = JfrAttributes.START_TIME.getAccessor(jfrEvent.getType());
            for (IItem item : jfrEvent) {
                for (Accessor accessor : accessors) {
                    IQuantity val = accessor.impl.getMember(item);
                    IQuantity time = timeAccessor != null ? timeAccessor.getMember(item) : null;
                    if (time != null && val != null) {
                        List<Object> list = (List<Object>) metrics.computeIfAbsent(accessor.name, k -> new ArrayList<>());
                        list.add(new Metric(val.doubleValue(), time.longValue()));
                    }
                }
            }
        }

        return metrics;
    }
}
