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

package org.bithon.server.topo.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:45
 */
@Data
public class Topo {
    private Set<EndpointBo> endpoints = new HashSet<>();
    private List<Link> links = new ArrayList<>();

    public EndpointBo getEndpointByName(String dstEndpoint) {
        return null;
    }

    public void addEndpoint(EndpointBo endpoint) {
        this.endpoints.add(endpoint);
    }

    public void addLink(Link link) {
        this.links.add(link);
    }
}
