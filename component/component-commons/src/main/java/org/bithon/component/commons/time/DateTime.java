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

package org.bithon.component.commons.time;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 5:20 下午
 */
public class DateTime {

    public static String toISO8601(long milliSeconds) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.ENGLISH).format(new Date(milliSeconds));
    }

    /**
     * @param timestamp timestamp in millisecond
     */
    public static String formatDateTime(String pattern, long timestamp) {
        return new SimpleDateFormat(pattern, Locale.ENGLISH).format(new Date(timestamp));
    }

    public static String toYYYYMMDDhhmmss(long timestamp) {
        return formatDateTime("yyyy-MM-dd HH:mm:ss", timestamp);
    }

    public static String toYYYYMMDDhhmmss(Timestamp timestamp) {
        return toYYYYMMDDhhmmss(timestamp.getTime());
    }

    public static String toYYYYMMDD(long timestamp) {
        return formatDateTime("yyyyMMdd", timestamp);
    }
}
