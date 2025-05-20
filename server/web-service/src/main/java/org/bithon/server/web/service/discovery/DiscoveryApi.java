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

package org.bithon.server.web.service.discovery;


import lombok.extern.slf4j.Slf4j;
import org.bithon.server.discovery.client.DiscoveredServiceInstance;
import org.bithon.server.discovery.client.IDiscoveryClient;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 20/5/25 8:48 pm
 */
@Slf4j
@CrossOrigin
@RestController
public class DiscoveryApi {
    private final IDiscoveryClient discoveryClient;

    public DiscoveryApi(IDiscoveryClient discoveryClient) {
        this.discoveryClient = discoveryClient;
    }

    @GetMapping("/api/discovery/instance")
    public List<DiscoveredServiceInstance> getInstanceList() {
        List<DiscoveredServiceInstance> instances = discoveryClient.getInstanceList();
        if (instances == null) {
            return List.of();
        } else {
            List<DiscoveredServiceInstance> instanceList = new ArrayList<>(instances);
            instanceList.sort(Comparator.comparing(DiscoveredServiceInstance::getServiceName).thenComparing(DiscoveredServiceInstance::getURL));
            return instanceList;
        }
    }
}
