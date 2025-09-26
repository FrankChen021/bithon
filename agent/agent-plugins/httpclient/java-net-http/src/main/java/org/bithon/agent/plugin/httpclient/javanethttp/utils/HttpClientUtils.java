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

package org.bithon.agent.plugin.httpclient.javanethttp.utils;


import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;

/**
 * @author frank.chen021@outlook.com
 * @date 26/9/25 10:27 am
 */
public class HttpClientUtils {
    public static long getRequestSize(HttpRequest request) {
        Optional<String> contentLength = request.headers().firstValue("content-length");
        if (contentLength.isPresent()) {
            try {
                return Long.parseLong(contentLength.get());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    public static long getResponseSize(HttpResponse<?> response) {
        Optional<String> contentLength = response.headers().firstValue("content-length");
        if (contentLength.isPresent()) {
            try {
                return Long.parseLong(contentLength.get());
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }
}
