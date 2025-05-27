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
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
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
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

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

        // Example: run asprof to profile current JVM for 30s and output HTML flamegraph
        String pid = String.valueOf(ProcessHandle.current().pid());
        // Create a directory for the UUID if it doesn't exist
        File dir = new File("/tmp/" + uuid);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new RuntimeException("Failed to create directory: " + dir.getAbsolutePath());
        }
        File uuidFile = new File(dir, "/%t.jfr");
        ProcessBuilder pb = new ProcessBuilder(
            profilerHome,
            "-d", "30",
            "--loop", "3s",
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

        WatchService watchService = FileSystems.getDefault().newWatchService();
        dir.toPath().register(watchService, StandardWatchEventKinds.ENTRY_CREATE);
        
        Queue<String> fileQueue = new LinkedList<>();
        Set<String> processedFiles = new HashSet<>();
        long startTime = System.currentTimeMillis();
        
        // First, add any existing JFR files to the queue (in case some were created before we started watching)
        File[] existingFiles = dir.listFiles((file, name) -> name.endsWith(".jfr"));
        if (existingFiles != null) {
            Arrays.sort(existingFiles, (f1, f2) -> f1.getName().compareTo(f2.getName()));
            for (File file : existingFiles) {
                fileQueue.offer(file.getName());
                log.debug("Added existing JFR file to queue: {}", file.getName());
            }
        }
        
        while (System.currentTimeMillis() < startTime + 35_000) { // Wait a bit longer than profiling duration
            try {
                // Wait for file system events (with timeout)
                WatchKey key = watchService.poll(1, TimeUnit.SECONDS);
                
                if (key != null) {
                    // Process file system events
                    for (WatchEvent<?> event : key.pollEvents()) {
                        WatchEvent<Path> ev = (WatchEvent<Path>) event;
                        String fileName = ev.context().toString();
                        
                        if (fileName.endsWith(".jfr")) {
                            // Add new JFR file to queue if not already present
                            if (!processedFiles.contains(fileName) && !fileQueue.contains(fileName)) {
                                fileQueue.offer(fileName);
                                log.info("New JFR file detected and added to queue: {}", fileName);
                            }
                        }
                    }
                    
                    if (!key.reset()) {
                        break;
                    }
                }
                
                // Process files from queue (regardless of whether we got events or not)
                processQueuedFiles(dir, fileQueue, processedFiles);
                
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
    }

    /**
     * Process files from the queue in chronological order
     */
    private void processQueuedFiles(File dir, Queue<String> fileQueue, Set<String> processedFiles) {
        // Process files from the front of the queue (oldest first)
        while (!fileQueue.isEmpty()) {
            String fileName = fileQueue.peek(); // Look at first file without removing it
            File jfrFile = new File(dir, fileName);
            
            // Skip if file doesn't exist anymore
            if (!jfrFile.exists()) {
                fileQueue.poll(); // Remove non-existent file from queue
                log.debug("Removed non-existent file from queue: {}", fileName);
                continue;
            }
            
            // Check if file is complete and ready for processing
            if (isFileComplete(jfrFile.toPath())) {
                // File is ready - process it and remove from queue
                log.info("Processing JFR file from queue: {}", fileName);
                processJfrFile(jfrFile.toPath());
                
                // Mark as processed and remove from queue
                processedFiles.add(fileName);
                fileQueue.poll(); // Now remove the processed file
                log.debug("Successfully processed and removed file from queue: {}", fileName);
            } else {
                // File is not ready yet - keep it in queue and stop processing
                // This maintains chronological order since we can't skip ahead
                log.debug("JFR file {} is not ready yet, keeping in queue and waiting...", fileName);
                break; // Important: do NOT call poll() here - keep file in queue
            }
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
