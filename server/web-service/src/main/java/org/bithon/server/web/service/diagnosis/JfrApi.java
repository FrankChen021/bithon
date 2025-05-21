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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 21/5/25 6:45 pm
 */
@Slf4j
@CrossOrigin
@RestController
public class JfrApi {

    public static void main(String[] args) throws IOException {
        JfrApi jfrApi = new JfrApi();
        jfrApi.analyzeJfrFile(new File("/Users/frank.chenling/Downloads/async-profiler-4.0-macos/bin/app.jfr"));
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

        // Example: run asprof to profile current JVM for 30s and output HTML flamegraph
        String pid = String.valueOf(ProcessHandle.current().pid());
        String uuidFile = "/tmp/" + uuid + ".html";
        ProcessBuilder pb = new ProcessBuilder(
            profilerHome,
            "-d", "30",
            "-f", uuidFile,
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

        try (InputStream in = file.getInputStream()) {
            return analyzeJfrFile(in);
        } catch (IOException e) {
            throw new RuntimeException(e);
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
