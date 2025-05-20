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

package org.bithon.server.discovery.client.k8s;


import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * @author frank.chen021@outlook.com
 * @date 20/5/25 2:41 pm
 */
@Getter
@Setter
@Configuration
@ConditionalOnProperty(prefix = "bithon.discovery.kubernetes", name = "enabled", havingValue = "true")
public class K8sDiscoveryProperties {
    /**
     * The Kubernetes namespace to look for services in.
     */
    private String namespace = "default";

    /**
     * The selector to use when looking for services.
     */
    private String serviceSelector = "";
}
