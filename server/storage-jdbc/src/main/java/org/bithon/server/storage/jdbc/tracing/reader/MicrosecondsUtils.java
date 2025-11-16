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

package org.bithon.server.storage.jdbc.tracing.reader;


import java.sql.Timestamp;
import java.time.LocalDateTime;

/**
 * @author frank.chen021@outlook.com
 * @date 1/11/25 12:42 pm
 */
public class MicrosecondsUtils {
    public static long toMicroseconds(Object val) {
        if (val instanceof LocalDateTime localDateTime) {
            val = Timestamp.valueOf(localDateTime);
        }
        if (val instanceof Timestamp timestamp) {
            return timestamp.getTime() * 1000 + timestamp.getNanos() / 1000;
        }
        if (val instanceof Long) {
            return (long) val;
        }
        throw new RuntimeException("Unknow type: " + val.getClass().getName());
    }

    public static LocalDateTime toLocalDateTime(long microseconds) {
        Timestamp ts = new Timestamp(microseconds / 1000);
        ts.setNanos((int) (microseconds % 1000) * 1000);
        return ts.toLocalDateTime();
    }
}
