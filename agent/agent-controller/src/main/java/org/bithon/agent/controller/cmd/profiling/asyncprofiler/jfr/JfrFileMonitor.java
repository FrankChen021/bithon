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

package org.bithon.agent.controller.cmd.profiling.asyncprofiler.jfr;

import org.bithon.agent.controller.cmd.profiling.asyncprofiler.ProgressNotifier;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.regex.Pattern;

/**
 * Monitors JFR files in a directory and provides them for processing when they're ready
 *
 * @author frank.chen021@outlook.com
 * @date 24/8/2025
 */
public class JfrFileMonitor {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(JfrFileMonitor.class);

    private final File fileDirectory;
    private final int fileDuration;
    private final BooleanSupplier cancellationCtrl;
    private final ProgressNotifier progressNotifier;
    private final Pattern timestampPattern = Pattern.compile("(\\d{8}-\\d{6})\\.jfr");
    private final PriorityQueue<TimestampedFile> queue = new PriorityQueue<>();
    private final Set<String> processedFiles = new HashSet<>();

    public JfrFileMonitor(File fileDirectory,
                          int fileDuration,
                          BooleanSupplier cancellationCtrl,
                          ProgressNotifier progressNotifier) {
        this.fileDirectory = fileDirectory;
        this.fileDuration = fileDuration;
        this.cancellationCtrl = cancellationCtrl;
        this.progressNotifier = progressNotifier;
    }

    /**
     * Polls for the next ready JFR file
     *
     * @return A ready JFR file with its size, or null if no file is ready or cancelled
     */
    public TimestampedFile poll() {
        while (!cancellationCtrl.getAsBoolean()) {
            scanNewFiles();

            // Then, check if any queued file is ready for processing
            TimestampedFile file = dequeue();
            if (file != null) {
                return file;
            }
            if (cancellationCtrl.getAsBoolean()) {
                return null;
            }

            progressNotifier.sendProgress("Waiting for new profiling data...");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ignored) {
                return null;
            }
        }
        return null;
    }

    private void scanNewFiles() {
        File[] files = fileDirectory.listFiles((file, name) -> name.endsWith(".jfr") && !processedFiles.contains(file.getName()));
        if (files == null) {
            return;
        }

        for (File file : files) {
            TimestampedFile timestampedFile = TimestampedFile.fromFile(file, timestampPattern);
            if (timestampedFile != null) {
                queue.offer(timestampedFile);
                progressNotifier.sendProgress("%s profiling data captured", timestampedFile.getName());
            }
        }
    }

    private TimestampedFile dequeue() {
        while (!queue.isEmpty() && !cancellationCtrl.getAsBoolean()) {
            TimestampedFile timestampedFile = queue.peek();

            //noinspection DataFlowIssue
            File jfrFilePath = timestampedFile.getPath();
            if (!jfrFilePath.exists()) {
                // Skip files that no longer exist
                processedFiles.add(jfrFilePath.getName());
                queue.poll();
                continue;
            }

            // Check if enough time has passed since the file's timestamp
            long now = System.currentTimeMillis();
            long fileTimestamp = timestampedFile.getTimestamp();
            long readyTimestamp = fileTimestamp + this.fileDuration * 1000L + 300; // Add 300ms buffer
            String readyTimeText = new SimpleDateFormat("HH:mm:ss", Locale.ENGLISH).format(new Date(readyTimestamp));

            long waitTime = readyTimestamp - now;
            while (waitTime > 0 && !cancellationCtrl.getAsBoolean() && !Thread.currentThread().isInterrupted()) {
                progressNotifier.sendProgress("%s NOT READY. Waiting to be ready until %s", timestampedFile.getName(), readyTimeText);

                long sleepTime = Math.min(waitTime, 1000); // Sleep in chunks of 1 second
                waitTime -= sleepTime;
                try {
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            if (cancellationCtrl.getAsBoolean()) {
                return null;
            }

            // Additional check: verify file is complete and stable
            long size = waitForCompleteAndReturnSize(jfrFilePath.toPath());
            if (size < 0) {
                continue;
            }

            // Pop the file from the queue - it's ready for processing
            queue.poll();

            processedFiles.add(jfrFilePath.getName());
            timestampedFile.setSize(size);
            return timestampedFile;
        }

        return null;
    }

    /**
     * Check if a file is complete by checking if its size is stable
     * This is a simple and reliable approach that works across all platforms
     */
    private long waitForCompleteAndReturnSize(Path filePath) {
        File file = filePath.toFile();

        // First check if file exists and has content
        if (!file.exists() || file.length() == 0) {
            return -1;
        }

        // Check if file size is stable (hasn't changed in the last 500ms)
        long size1 = file.length();
        long lastModified1 = file.lastModified();

        try {
            Thread.sleep(200); // Wait 200ms
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return -1;
        }

        long size2 = file.length();
        long lastModified2 = file.lastModified();

        // File is complete if size and last modified time haven't changed
        boolean isStable = (size1 == size2) && (lastModified1 == lastModified2) && (size1 > 0);
        if (!isStable) {
            LOG.debug("File {} is still changing: size {} -> {}, lastModified {} -> {}",
                      filePath, size1, size2, lastModified1, lastModified2);
        }

        return isStable ? size1 : -1;
    }
}
