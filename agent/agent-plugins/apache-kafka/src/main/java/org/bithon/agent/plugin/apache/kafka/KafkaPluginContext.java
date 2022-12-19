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

package org.bithon.agent.plugin.apache.kafka;

import org.bithon.agent.core.context.InterceptorContext;

import java.util.function.Supplier;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/12/3 15:53
 */
public class KafkaPluginContext {
    /**
     * Available for consumer.
     * default to empty string so that no need to handle null.
     */
    public String groupId = "";
    public String clientId;
    public Supplier<String> clusterSupplier;

    public static String getCurrentDestination() {
        String dest = (String) InterceptorContext.get("kafka-ctx-destination");
        return dest == null ? "" : dest;
    }

    public static void setCurrentDestination(String destination) {
        InterceptorContext.set("kafka-ct-destination", destination);
    }

    public static void resetCurrentDestination() {
        InterceptorContext.remove("kafka-ctx-destination");
    }

}
