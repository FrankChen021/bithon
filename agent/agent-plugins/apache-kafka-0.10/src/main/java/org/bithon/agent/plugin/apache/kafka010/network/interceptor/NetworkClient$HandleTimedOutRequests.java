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

package org.bithon.agent.plugin.apache.kafka010.network.interceptor;

import org.apache.kafka.clients.ClientResponse;
import org.apache.kafka.common.protocol.ApiKeys;
import org.bithon.agent.plugin.apache.kafka.network.interceptor.NetworkClient$CompleteResponses;

import java.util.List;

/**
 * {@link org.apache.kafka.clients.NetworkClient#handleCompletedReceives(List, long)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/4 17:13
 */
public class NetworkClient$HandleTimedOutRequests extends NetworkClient$CompleteResponses {

    @Override
    protected ApiKeys getResponseApiKeys(ClientResponse response) {
        short key = response.request().request().header().apiKey();
        return ApiKeys.forId(key);
    }

    @Override
    protected String getResponseNodeId(ClientResponse response) {
        return response.request().request().destination();
    }

    @Override
    protected String getException(ClientResponse response) {
        return "";
    }
}