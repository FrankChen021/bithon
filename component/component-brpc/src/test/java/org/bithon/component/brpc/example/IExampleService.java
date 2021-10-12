/*
 *    Copyright 2020 bithon.cn
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

package org.bithon.component.brpc.example;

import org.bithon.component.brpc.ServiceConfig;
import org.bithon.component.brpc.example.protobuf.WebRequestMetrics;
import org.bithon.component.brpc.message.serializer.Serializer;

import java.util.List;
import java.util.Map;

public interface IExampleService {
    int div(int a, int b);

    /**
     * timeout in seconds
     */
    void block(int timeout);

    /**
     * Oneway test
     */
    @ServiceConfig(isOneway = true)
    void sendOneway(String msg);

    /**
     * test composite type
     */
    int[] append(int[] arrays, int value);

    String[] append(String[] arrays, String value);

    /**
     * test composite type
     */
    List<String> delete(List<String> list, int index);

    /**
     * test composite type
     */
    Map<String, String> merge(Map<String, String> a, Map<String, String> b);

    String send(WebRequestMetrics metrics);

    /**
     * test multiple protobuf messages
     */
    String send(WebRequestMetrics metrics1, WebRequestMetrics metrics2);

    String send(String uri, WebRequestMetrics metrics);

    String send(WebRequestMetrics metrics, String uri);

    @ServiceConfig(serializer = Serializer.JSON, name = "merge2")
    Map<String, String> mergeWithJson(Map<String, String> a, Map<String, String> b);

    /**
     * empty arg test
     */
    String ping();
}
