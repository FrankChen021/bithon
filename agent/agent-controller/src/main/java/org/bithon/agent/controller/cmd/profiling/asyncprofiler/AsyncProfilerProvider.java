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
import org.bithon.agent.controller.cmd.profiling.IProfilerProvider;
import org.bithon.agent.controller.cmd.profiling.ProfilingException;
import org.bithon.agent.rpc.brpc.profiling.ProfilingEvent;
import org.bithon.agent.rpc.brpc.profiling.ProfilingRequest;
import org.bithon.component.brpc.StreamResponse;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * The class provides the profiling implementation based on async-profiler.
 *
 * @author frank.chen021@outlook.com
 * @date 19/8/25 3:56 pm
 */
public class AsyncProfilerProvider implements IProfilerProvider {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(AsyncProfilerProvider.class);

    private static volatile boolean isProfilingRunning = false;

    @Override
    public void start(ProfilingRequest request, StreamResponse<ProfilingEvent> streamResponse) {
        if (request.getIntervalInSeconds() <= 0 || request.getDurationInSeconds() <= 0) {
            throw new ProfilingException("Interval and duration must be greater than 0");
        }

        // Create a progress notifier
        ProgressNotifier progressNotifier = new ProgressNotifier(streamResponse);
        progressNotifier.sendProgress("Validating if profiling events are supported for this application...");

        // Validate requested events
        try {
            String[] availableEvents = AsyncProfiler.getInstance().execute("list").split("\n");
            Set<String> supportedEvents = new HashSet<>();
            for (String line : availableEvents) {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("Basic events:") && !line.startsWith("Java method")) {
                    supportedEvents.add(line);
                }
            }

            Set<String> unsupportedEvents = new HashSet<>(request.getProfileEventsList());
            unsupportedEvents.removeAll(supportedEvents);
            if (!unsupportedEvents.isEmpty()) {
                throw new ProfilingException(StringUtils.format("Unsupported profiling events: %s. Supported events: %s",
                                                                String.join(", ", unsupportedEvents),
                                                                String.join(", ", supportedEvents)));
            }
        } catch (IOException e) {
            throw new ProfilingException("Failed to check supported events: " + e.getMessage(), e);
        }

        // Even though async-profiler guarantees that only one instance is running at a time,
        // but it might take a while to stop the previous profiling session completely if the profiling is closed from the client
        if (isProfilingRunning) {
            throw new ProfilingException("Another profiling task is still running.");
        } else {
            synchronized (AsyncProfilerProvider.class) {
                if (isProfilingRunning) {
                    throw new ProfilingException("Another profiling task is still running.");
                }
                isProfilingRunning = true;
            }
        }

        Thread proflingThread = new Thread(() -> {
            // Configuration
            int profilingDuration = request.getDurationInSeconds();
            int profilingInterval = request.getIntervalInSeconds();
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (profilingDuration + profilingInterval + 3) * 1000L;

            //
            // Start profiling and streaming
            //
            AsyncProfilerTask task = null;
            try {
                task = new AsyncProfilerTask(request, endTime, streamResponse);
                task.run();
            } catch (Throwable e) {
                streamResponse.onException(e);
            } finally {
                isProfilingRunning = false;

                // Stop the profiler if task was created
                if (task != null) {
                    task.stopProfiling();
                }
                LOG.info("Stopped profiling");
            }
        });
        proflingThread.setName("bithon-profiler");
        proflingThread.setDaemon(true);
        proflingThread.setUncaughtExceptionHandler((t, e) -> isProfilingRunning = false);
        proflingThread.start();
    }
}
