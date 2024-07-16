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
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.springframework.http.HttpStatus;

import java.util.List;

/**
 * @author frankchen
 */
public class NacosDiscoveryClient implements IDiscoveryClient {
    private final NacosServiceDiscovery discovery;
    private final NacosDiscoveryProperties props;

    public NacosDiscoveryClient(NacosServiceDiscovery discovery, NacosDiscoveryProperties props) {
        this.discovery = discovery;
        this.props = props;
    }

    @Override
    public List<DiscoveredServiceInstance> getInstanceList(String serviceName) {
        try {
            List<DiscoveredServiceInstance> instanceList = discovery.getInstances(props.getService())
                                                                    .stream()
                                                                    .filter(serviceInstance -> serviceInstance.getMetadata().containsKey("bithon.service." + serviceName))
                                                                    .map(serviceInstance -> new DiscoveredServiceInstance(serviceInstance.getHost(), serviceInstance.getPort()))
                                                                    .toList();
            if (instanceList.isEmpty()) {
                throw new HttpMappableException(HttpStatus.SERVICE_UNAVAILABLE.value(), "Not found any instance of service [%s]", serviceName);
            }

            return instanceList;
        } catch (NacosException e) {
            throw new HttpMappableException(e,
                                            HttpStatus.INTERNAL_SERVER_ERROR.value(),
                                            "Failed to found any instances of service [%s]: [%s]", serviceName, e.getMessage());
        }
    }
}
