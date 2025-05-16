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

package org.bithon.server.storage.jdbc.clickhouse.common.exception;

import java.net.UnknownHostException;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/1/14 11:57
 */
public class RetryableExceptions {
    public static boolean isExceptionRetryable(Exception e) {
        if (e instanceof UnknownHostException) {
            return true;
        }
        String message = e.getMessage();
        return message != null
               && (message.startsWith("Connect timed out")
                   || message.contains("connect timed out")
                   || message.contains("Connection reset")
                   || message.contains("Connection refused")
                   || message.startsWith("Unexpected end of file from server")
                   // The following is thrown from sun.net.www.protocol.http.HTTPURLConnection
                   || message.startsWith("Error writing request body to server")
                   || message.contains("The target server failed to respond")
               );
    }
}
