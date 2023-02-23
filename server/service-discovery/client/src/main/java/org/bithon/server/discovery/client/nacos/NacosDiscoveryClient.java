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

package org.bithon.server.discovery.client.nacos;

import com.alibaba.cloud.nacos.NacosDiscoveryProperties;
import com.alibaba.cloud.nacos.discovery.NacosServiceDiscovery;
import com.alibaba.nacos.api.exception.NacosException;
import org.bithon.server.discovery.client.IDiscoveryClient;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frankchen
 */
public class NacosDiscoveryClient implements IDiscoveryClient {
    private final NacosServiceDiscovery discovery;
    private final NacosDiscoveryProperties prop;

    public NacosDiscoveryClient(NacosServiceDiscovery discovery, NacosDiscoveryProperties prop) {
        this.discovery = discovery;
        this.prop = prop;
    }

    @Override
    public List<HostAndPort> getInstanceList(String serviceName) {
        try {
            return discovery.getInstances(prop.getService()).stream()
                            .filter(serviceInstance -> serviceInstance.getMetadata().containsKey("bithon.service." + serviceName))
                            .map(serviceInstance -> new HostAndPort(serviceInstance.getHost(), serviceInstance.getPort()))
                            .collect(Collectors.toList());
        } catch (NacosException e) {
            return Collections.emptyList();
        }
    }
}
