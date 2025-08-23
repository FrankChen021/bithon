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
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

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
        } catch (Throwable e) {
            streamResponse.onException(e);
        } finally {
            isProfiling = false;
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
        ProgressNotifier progressNotifier = new ProgressNotifier(streamResponse);
        progressNotifier.sendProgress("Validating if target application supports profiling events");
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
                this.tryStopProfiling(toolLocation, pid);

                AsyncProfilerTask task = new AsyncProfilerTask(toolLocation, pid, dir, request, endTime, streamResponse);
                task.run();
            } catch (Throwable e) {
                streamResponse.onException(e);
            } finally {
                isProfiling = false;

                // Terminate the profiler process after the duration
                tryStopProfiling(toolLocation, pid);
                LOG.info("Stopped profiling for PID {}", pid);

                // Cleanup
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
}
