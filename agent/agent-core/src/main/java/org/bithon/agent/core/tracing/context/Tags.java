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

package org.bithon.agent.core.tracing.context;

/**
 * @author Frank Chen
 * @date 25/12/21 5:56 PM
 */
public class Tags {
    public static final String HTTP_VERSION = "http.version";
    public static final String HTTP_METHOD = "http.method";

    /**
     * For a {@link SpanKind#CLIENT}, the uri must be in the format of URI where the scheme represent the target service
     *
     * For example,
     *  http://localhost:8080
     */
    public static final String HTTP_URI = "http.uri";
    public static final String HTTP_STATUS = "http.status";
    public static final String SQL = "sql";

    /**
     *  redis://127.0.0.1:6379
     *  mongodb://127.0.0.1:8000
     *  mysql://127.0.0.1:3309
     */
    public static final String TARGET = "target";
}
