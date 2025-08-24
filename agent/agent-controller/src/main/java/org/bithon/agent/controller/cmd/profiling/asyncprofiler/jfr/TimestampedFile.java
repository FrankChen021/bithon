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
import java.time.LocalDateTime;
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
        LocalDateTime dateTime = LocalDateTime.of(year, month, day, hour, minute, second);
        return dateTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
