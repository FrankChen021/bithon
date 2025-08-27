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


import java.io.File;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a JFR file with its extracted timestamp
 */
public class TimestampedFile implements Comparable<TimestampedFile> {
    private final File path;
    private final String name;
    private final long timestamp;
    private long size;

    private static final ZoneId HOST_ZONE;

    static {
        ZoneId zone = null;

        // 1) TZ environment variable has the highest priority (same as libc)
        String tz = System.getenv("TZ");
        if (tz != null && !tz.isEmpty()) {
            try {
                zone = ZoneId.of(tz);
            } catch (Exception ignored) {
            }
        }

        // 2) /etc/localtime symlink
        if (zone == null) {
            Path localtime = Paths.get("/etc/localtime");
            if (Files.isSymbolicLink(localtime)) {
                try {
                    Path target = Files.readSymbolicLink(localtime);
                    String id = target.toString().replace("\\", "/");
                    // Strip any leading path up to and including the "/zoneinfo/" directory so that
                    // we end up with the pure zone ID like "Asia/Singapore".
                    final String marker = "/zoneinfo/";
                    int idx = id.indexOf(marker);
                    if (idx >= 0) {
                        id = id.substring(idx + marker.length());
                    }
                    zone = ZoneId.of(id);
                } catch (Exception ignored) {
                }
            }
        }

        // 3) /etc/timezone plain-text file (Debian/Ubuntu)
        if (zone == null) {
            Path tzFile = Paths.get("/etc/timezone");
            if (Files.isReadable(tzFile)) {
                try {
                    byte[] content = Files.readAllBytes(tzFile);
                    zone = ZoneId.of(new String(content, Charset.defaultCharset()).trim());
                } catch (Exception ignored) {
                }
            }
        }

        if (zone == null) {
            zone = ZoneOffset.UTC;
        }

        HOST_ZONE = zone;
    }

    private TimestampedFile(File path, String name, long timestamp) {
        this.path = path;
        this.name = name;
        this.timestamp = timestamp;
        this.size = -1;
    }

    /**
     * name without extension
     */
    public String getName() {
        return name;
    }

    public File getPath() {
        return path;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    @Override
    public String toString() {
        return path.getPath();
    }

    @Override
    public int compareTo(TimestampedFile that) {
        return Long.compare(this.timestamp, that.timestamp);
    }

    public static TimestampedFile fromFile(File file, Pattern timestampPattern) {
        Matcher matcher = timestampPattern.matcher(file.getName());
        if (matcher.find()) {
            String timestampText = matcher.group(1);

            // Parse timestamp from format YYYYMMDD-HHMMSS
            return new TimestampedFile(file, timestampText, parseTimestamp(timestampText));
        }
        return null;
    }

    private static long parseTimestamp(String timestampText) {
        // Parse YYYYMMDD-HHMMSS format directly from the text
        int year = Integer.parseInt(timestampText.substring(0, 4));
        int month = Integer.parseInt(timestampText.substring(4, 6));
        int day = Integer.parseInt(timestampText.substring(6, 8));
        int hour = Integer.parseInt(timestampText.substring(9, 11));
        int minute = Integer.parseInt(timestampText.substring(11, 13));
        int second = Integer.parseInt(timestampText.substring(13, 15));

        // According to async-profiler's implementation, the timestamp embedded in the JFR filename is
        // always generated in UTC (see Timestamp::timeToString in async-profiler's source).
        // If we convert it using the system default time-zone, the value will drift when the
        // application is running in a non-UTC zone, which causes the monitor to think the file is
        // older than it actually is and start consuming it immediately.
        return LocalDateTime.of(year, month, day, hour, minute, second)
                            .atZone(HOST_ZONE)
                            .toInstant()
                            .toEpochMilli();
    }
}
