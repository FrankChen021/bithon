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

import java.io.File;
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

    private AsyncProfilerTask task = null;

    public AsyncProfilerProvider() {
        // Set the extraction path for async-profiler
        // because it does not handle relative directory correctly, see: https://github.com/async-profiler/async-profiler/issues/1451
        File location = new File(System.getProperty("java.io.tmpdir", "/tmp"), "org.bithon.agent/profiling/async-profiler");
        if (!location.exists() && !location.mkdirs()) {
            throw new ProfilingException("Failed to prepare directory: " + location.getAbsolutePath());
        }
        System.setProperty("one.profiler.extractPath", location.getAbsolutePath());

        // Initialize async-profiler
        AsyncProfiler.getInstance();
    }

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
        // So we still need to check if there's another profiling task is running
        if (task != null) {
            throw new ProfilingException("Another profiling task is still running.");
        } else {
            synchronized (AsyncProfilerProvider.class) {
                if (task != null) {
                    throw new ProfilingException("Another profiling task is still running.");
                }
                task = new AsyncProfilerTask(request, streamResponse);
            }
        }

        Thread proflingThread = new Thread(() -> {
            try {
                task.run();
            } catch (Throwable e) {
                streamResponse.onException(e);
            } finally {
                task.stopProfiling();

                task = null;
                LOG.info("Stopped profiling");
            }
        });
        proflingThread.setName("bithon-profiler");
        proflingThread.setDaemon(true);
        proflingThread.setUncaughtExceptionHandler((t, e) -> task = null);
        proflingThread.start();
    }

    @Override
    public void stop() {
        if (task != null) {
            task.stop();
        }
    }

    @Override
    public boolean isRunning() {
        return task != null;
    }
}
