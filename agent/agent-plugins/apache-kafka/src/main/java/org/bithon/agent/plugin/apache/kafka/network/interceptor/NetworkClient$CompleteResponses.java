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

package org.bithon.agent.plugin.apache.kafka.network.interceptor;

import org.apache.kafka.clients.ClientResponse;
import org.apache.kafka.common.protocol.ApiKeys;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.observability.metric.collector.MetricRegistryFactory;
import org.bithon.agent.plugin.apache.kafka.KafkaPluginContext;
import org.bithon.agent.plugin.apache.kafka.network.metrics.NetworkMetricRegistry;
import org.bithon.agent.plugin.apache.kafka.network.metrics.NetworkMetrics;
import org.bithon.component.commons.utils.ReflectionUtils;

import java.lang.reflect.Field;
import java.util.List;

/**
 * {@link org.apache.kafka.clients.NetworkClient#completeResponses(List)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/12/4 17:13
 */
public class NetworkClient$CompleteResponses extends AfterInterceptor {

    private final NetworkMetricRegistry metricRegistry;
    private Field authenticationExceptionField;

    public NetworkClient$CompleteResponses() {
        metricRegistry = MetricRegistryFactory.getOrCreateRegistry("kafka-network",
                                                                   NetworkMetricRegistry::new);

        try {
            authenticationExceptionField = ReflectionUtils.getField(ClientResponse.class, "authenticationException");
            authenticationExceptionField.setAccessible(true);
        } catch (NoSuchFieldException ignored) {
        }
    }

    @Override
    public void after(AopContext aopContext) {
        KafkaPluginContext pluginContext = aopContext.getInjectedOnTargetAs();

        List<ClientResponse> responses = aopContext.getArgAs(0);
        for (ClientResponse response : responses) {

            ApiKeys apiKeys = response.requestHeader().apiKey();
            String messageType = apiKeys.name;
            String nodeId = response.destination();

            // Process the node id for messages sending to group coordinator
            if (apiKeys.id == ApiKeys.LEAVE_GROUP.id
                    || apiKeys.id == ApiKeys.JOIN_GROUP.id
                    || apiKeys.id == ApiKeys.OFFSET_COMMIT.id
                    || apiKeys.id == ApiKeys.OFFSET_FETCH.id
                    || apiKeys.id == ApiKeys.HEARTBEAT.id) {

                try {
                    // See FindCoordinatorResponseHandler in AbstractCoordinator
                    int realNodeId = Integer.MAX_VALUE - Integer.parseInt(nodeId);
                    nodeId = String.valueOf(realNodeId);
                } catch (NumberFormatException ignored) {
                }
            }

            String exceptionName = response.wasDisconnected() ? "Disconnected" : "";

            // authenticationException is only support in some higher version(at least 1.0 version has no such exception)
            // So, we need to use reflection to get the value so that this code is compatible with these old Kafka clients
            Exception exception = null;
            if (authenticationExceptionField != null) {
                try {
                    exception = (Exception) authenticationExceptionField.get(response);
                } catch (IllegalAccessException ignored) {
                }
            }
            if (exception == null) {
                exception = response.versionMismatch();
            }
            if (exception != null) {
                exceptionName = exception.getClass().getSimpleName();
            }

            NetworkMetrics metrics = metricRegistry.getOrCreateMetrics(messageType,
                                                                       pluginContext.clusterSupplier.get(),
                                                                       nodeId,
                                                                       pluginContext.groupId,
                                                                       pluginContext.clientId,
                                                                       exceptionName);

            long latency = response.requestLatencyMs();
            metrics.requestCount.update(1);
            metrics.minResponseTime.update(latency);
            metrics.responseTime.update(latency);
            metrics.maxResponseTime.update(latency);
        }
    }
}
