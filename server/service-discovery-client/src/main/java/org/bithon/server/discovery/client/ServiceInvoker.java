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

package org.bithon.server.discovery.client;

import org.bithon.server.discovery.declaration.DiscoveredService;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 23/2/23 12:47 am
 */
@Service
public class ServiceInvoker {
    private final IDiscoveryClient client;

    public ServiceInvoker(IDiscoveryClient client) {
        this.client = client;
    }

    public <T> T create(Class<T> clazz) {
//        DiscoveredService annotation = clazz.getAnnotation(DiscoveredService.class);
//        if (annotation == null) {
//
//        }
//        List<IDiscoveryClient.HostAndPort> instanceList = client.getInstanceList(annotation.name());
        return createProxy(serviceName, clazz);
    }
}
