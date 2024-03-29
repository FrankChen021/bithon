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

package org.bithon.server.webapp.services;

import org.bithon.server.webapp.WebAppModuleEnabler;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/8 8:13 下午
 */
@Service
@Conditional(value = WebAppModuleEnabler.class)
public class ServiceDiscovery {
    private String apiHost;

    public ServiceDiscovery(ApplicationContext context) {
        apiHost = System.getProperty("bithon.api.host");
        if (!StringUtils.isEmpty(apiHost)) {
            return;
        }

        // check if webapp is running together with web-service
        if (!context.containsBean("dataSourceApi")) {
            //TODO: use service discovery
            throw new IllegalStateException("-Dbithon.api.host not specified for web-app");
        }

        // if this service is deployed in docker, the IP we get from the current interface is an internal IP
        // So, we don't need to set the apiHost
        apiHost = "";
    }

    public String getApiHost() {
        return apiHost;
    }

}
