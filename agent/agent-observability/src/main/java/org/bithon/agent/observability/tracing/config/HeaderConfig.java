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

package org.bithon.agent.observability.tracing.config;

import org.bithon.agent.configuration.annotation.PropertyDescriptor;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/8/6 21:09
 */
public class HeaderConfig {
    @PropertyDescriptor(
        description = "The HTTP headers that will be added to the tracing span logs when a HTTP request is received from a client. ",
        required = false
    )
    private List<String> request = Collections.emptyList();

    @PropertyDescriptor(
        description = "The HTTP headers that will be added to the tracing span logs when a HTTP response is received from a remote HTTP server.",
        required = false
    )
    private List<String> response = Collections.emptyList();

    public List<String> getRequest() {
        return request;
    }

    public void setRequest(List<String> request) {
        // Headers in HTTP protocols are case-insensitive
        this.request = request.stream()
                              .map((header) -> header.toLowerCase(Locale.ENGLISH))
                              .collect(Collectors.toList());
    }

    public List<String> getResponse() {
        return response;
    }

    public void setResponse(List<String> response) {
        // Headers in HTTP protocols are case-insensitive
        this.response = response.stream()
                                .map((header) -> header.toLowerCase(Locale.ENGLISH))
                                .collect(Collectors.toList());
    }
}
