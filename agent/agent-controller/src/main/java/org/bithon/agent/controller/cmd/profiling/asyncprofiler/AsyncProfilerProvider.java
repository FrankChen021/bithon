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


import org.bithon.agent.controller.cmd.profiling.IProfilerProvider;
import org.bithon.agent.controller.cmd.profiling.ProfilingException;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.JfrFileConsumer;
import org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr.TimestampedFile;
import org.bithon.agent.instrumentation.utils.AgentDirectory;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.agent.rpc.brpc.profiling.Progress;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.time.Clock;
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
 * @date 19/8/25 3:56 pm
 */
public class AsyncProfilerProvider implements IProfilerProvider {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AsyncProfilerProvider.class);

    /**
     * Make sure only one profiling session can run at a time.
     */
    private static volatile boolean isProfiling = false;

    private final Clock clock = new Clock();

    @Override
    public void start(ProfilingRequest request, StreamResponse<ProfilingEvent> streamResponse) {
        synchronized (AsyncProfilerProvider.class) {
            if (isProfiling) {
                streamResponse.onException(new ProfilingException("A profiling session is already running"));
                return;
            }

            isProfiling = true;
        }
        try {
            startProfilingTask(request, streamResponse);
        } catch (Exception e) {
            isProfiling = false;
            streamResponse.onException(e);
        }
    }

    private void startProfilingTask(ProfilingRequest request, StreamResponse<ProfilingEvent> streamResponse) {
        if (request.getIntervalInSeconds() <= 0 || request.getDurationInSeconds() <= 0) {
            throw new ProfilingException("Interval and duration must be greater than 0");
        }

        // Determine OS and arch
        String toolLocation = getToolLocation();
        String pid = ManagementFactory.getRuntimeMXBean().getName().split("@")[0];

        //
        // Check if requested events are supported
        //
        sendProgress(streamResponse, "Validating supported profiling events" + pid);
        {
            Set<String> supportedEvents = getSupportedEvents(toolLocation, pid);
            Set<String> unsupportedEvents = new HashSet<>(request.getProfileEventsList());
            unsupportedEvents.removeAll(supportedEvents);
            if (!unsupportedEvents.isEmpty()) {
                throw new ProfilingException(StringUtils.format("Unsupported profiling events: %s. Supported events: %s",
                                                                String.join(", ", unsupportedEvents),
                                                                String.join(", ", supportedEvents)));
            }
        }

        String uuid = UUIDv7Generator.create(UUIDv7Generator.INCREMENT_TYPE_DEFAULT)
                                     .generate()
                                     .toCompactFormat();
        File dir = new File(System.getProperty("java.io.tmpdir", "/tmp"), uuid);
        if (!dir.exists() && !dir.mkdirs()) {
            throw new ProfilingException("Failed to create temporary directory: " + dir.getAbsolutePath());
        }

        // Configuration
        int durationSeconds = request.getDurationInSeconds();
        int intervalSeconds = request.getIntervalInSeconds();
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (durationSeconds + intervalSeconds + 3) * 1000L;

        Thread proflingThread = new Thread(() -> {
            try {
                tryStopProfiling(toolLocation, pid);

                sendProgress(streamResponse, "Starting profiling for PID " + pid);
                startProfiling(toolLocation, pid, durationSeconds, intervalSeconds, dir, String.join(",", request.getProfileEventsList()));

                collectProfilingData(dir, intervalSeconds, endTime, streamResponse);

                streamResponse.onComplete();
            } catch (Exception e) {
                streamResponse.onException(e);
            } finally {
                isProfiling = false;

                // Cleanup
                LOG.debug("File processor thread completed, deleting directory: {}", dir.getAbsolutePath());
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

                // Terminate the profiler process after the duration
                sendProgress(streamResponse, "Stopping profiler for PID {}", pid);
                tryStopProfiling(toolLocation, pid);
            }
        });
        proflingThread.setName("bithon-profiler");
        proflingThread.setDaemon(true);
        proflingThread.start();
    }

    private void tryStopProfiling(String toolLocation, String pid) {
        try {
            Process process = new ProcessBuilder(toolLocation, "stop", pid).start();
            process.waitFor();
        } catch (IOException | InterruptedException ignored) {
        }
    }

    private void startProfiling(String toolLocation,
                                String pid,
                                long durationSecond,
                                long intervalSecond,
                                File outputDir,
                                String requestedEvents) {
        // Use the requested events or default to "cpu" if none specified
        String events = requestedEvents.isEmpty() ? "cpu" : requestedEvents;

        ProcessBuilder pb = new ProcessBuilder(toolLocation,
                                               "-d", String.valueOf(durationSecond),
                                               "--loop", intervalSecond + "s",
                                               "-f", new File(outputDir, "%t.jfr").getAbsolutePath(),
                                               "-e", events,
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
        LOG.info("Started profiling for {} seconds", durationSecond);
    }

    private void collectProfilingData(File dir,
                                      int loopIntervalSeconds,
                                      long endTime,
                                      StreamResponse<ProfilingEvent> streamResponse) {
        Set<String> skipped = new HashSet<>();

        // Pattern to extract timestamp from filename (format: YYYYMMDD-HHMMSS.jfr)
        Pattern timestampPattern = Pattern.compile("(\\d{8}-\\d{6})\\.jfr");

        PriorityQueue<TimestampedFile> queue = new PriorityQueue<>();
        while (System.currentTimeMillis() < endTime && !streamResponse.isCancelled()) {
            File[] files = dir.listFiles((file, name) -> name.endsWith(".jfr"));
            if (files != null) {
                for (File file : files) {
                    if (skipped.contains(file.getAbsolutePath())) {
                        continue;
                    }
                    TimestampedFile timestampedFile = TimestampedFile.fromFile(file, timestampPattern);
                    if (timestampedFile != null) {
                        queue.offer(timestampedFile);
                        sendProgress(streamResponse, "%s captured, timestamp = %d", file.getName(), timestampedFile.getTimestamp());
                    }
                }
            }

            while (!queue.isEmpty() && !streamResponse.isCancelled()) {
                TimestampedFile timestampedFile = queue.peek();

                //noinspection DataFlowIssue
                File jfrFile = timestampedFile.getFile();

                // Skip if file doesn't exist anymore
                if (!jfrFile.exists()) {
                    queue.poll();
                    continue;
                }

                //
                // Check if enough time has passed since the file's timestamp
                //
                String jfrFileName = jfrFile.getName();
                long currentTime = System.currentTimeMillis();
                long fileTimestamp = timestampedFile.getTimestamp();
                long readyTime = fileTimestamp + loopIntervalSeconds * 1000L + 500; // Add 500ms buffer
                long waitTime = readyTime - currentTime;
                while (waitTime > 0 && !streamResponse.isCancelled() && !Thread.currentThread().isInterrupted()) {
                    sendProgress(streamResponse, "%s is under generation. Waiting...", jfrFileName);

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
                if (!waitForComplete(jfrFile.toPath())) {
                    continue;
                }

                // Pop the file from the queue
                queue.poll();

                try {
                    sendProgress(streamResponse, "%s is ready. Now streaming profiling data...", jfrFileName);
                    JfrFileConsumer.consume(jfrFile,
                                            new JfrFileConsumer.EventConsumer() {
                                                @Override
                                                public void onEvent(ProfilingEvent event) {
                                                    streamResponse.onNext(event);
                                                }

                                                @Override
                                                public boolean isCancelled() {
                                                    return streamResponse.isCancelled();
                                                }
                                            }
                    );
                } catch (IOException e) {
                    sendProgress(streamResponse, "Failed to read profiling events from file %s: %s", jfrFileName, e.getMessage());
                } finally {
                    if (!jfrFile.delete()) {
                        LOG.warn("Failed to delete JFR file: {}", jfrFileName);
                        skipped.add(jfrFile.getAbsolutePath());
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

    /**
     * return the supported event names by the target application. The tool returns an example as follows:
     * Basic events:
     * cpu
     * alloc
     * nativemem
     * lock
     * wall
     * itimer
     * Java method calls:
     * ClassName.methodName
     *
     * @param toolLocation The location of the async-profiler tool
     * @return A set of supported event names
     */
    private Set<String> getSupportedEvents(String toolLocation, String pid) {
        Set<String> supportedEvents = new HashSet<>();
        try {
            Process process = new ProcessBuilder(toolLocation, "list", pid).start();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.length() > 2 && line.charAt(0) == ' ' && line.charAt(1) == ' ' && Character.isAlphabetic(line.charAt(2))) {
                        String eventName = line.substring(2).trim();
                        if (!eventName.isEmpty()) {
                            supportedEvents.add(eventName);
                        }
                    }
                }
            }

            try {
                process.waitFor();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }

            if (process.exitValue() != 0) {
                throw new ProfilingException(StringUtils.format("Failed to get supported events. Exit code: %d", process.exitValue()));
            }

            if (supportedEvents.isEmpty()) {
                throw new ProfilingException("Failed to get supported events.");
            }

            return supportedEvents;
        } catch (IOException e) {
            throw new ProfilingException(StringUtils.format("Failed to get supported events: %s", e.getMessage()));
        }
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
            throw new ProfilingException(StringUtils.format("The profiling does not support the this system. os = %s, arch = %s ", os, arch));
        }

        File toolHome = AgentDirectory.getSubDirectory("tools/async-profiler");
        if (!toolHome.exists()) {
            throw new ProfilingException("Cannot find the profiling tool at [%s]. Please report it to the agent maintainers." + toolHome.getAbsolutePath());
        }
        File osDir = new File(toolHome, dir);
        if (!osDir.exists()) {
            throw new ProfilingException("Cannot find the profiling tool at [%s]. Please report it to the agent maintainers. " + osDir.getAbsolutePath());
        }
        File toolPath = new File(osDir, "bin/asprof");
        if (!toolPath.exists()) {
            throw new ProfilingException(StringUtils.format("Cannot locate the profiling tool at [%s]. Please report it to agent maintainers.", toolPath));
        }
        return toolPath.getAbsolutePath();
    }

    private void sendProgress(StreamResponse<ProfilingEvent> streamResponse, String message) {
        ProfilingEvent event = ProfilingEvent.newBuilder()
                                             .setProgress(Progress.newBuilder()
                                                                  .setTime(clock.currentNanoseconds())
                                                                  .setMessage(message)
                                                                  .build())
                                             .build();
        streamResponse.onNext(event);
    }

    private void sendProgress(StreamResponse<ProfilingEvent> streamResponse, String messageFormat, Object... args) {
        ProfilingEvent event = ProfilingEvent.newBuilder()
                                             .setProgress(Progress.newBuilder()
                                                                  .setTime(clock.currentNanoseconds())
                                                                  .setMessage(StringUtils.format(messageFormat, args))
                                                                  .build())
                                             .build();
        streamResponse.onNext(event);
    }
}
