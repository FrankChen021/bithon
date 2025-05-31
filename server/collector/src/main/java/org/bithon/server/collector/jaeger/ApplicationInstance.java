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

package org.bithon.server.collector.jaeger;


import io.jaegertracing.thriftjava.Batch;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 31/5/25 12:56 pm
 */
@Getter
@AllArgsConstructor
public class ApplicationInstance {
    private String applicationName;
    private String instanceName;

    public static ApplicationInstance from(Batch batch) {
        if (batch.getProcess() == null) {
            return new ApplicationInstance("Unknown", "");
        }

        String appName = batch.getProcess().getServiceName();
        String instanceName = "";
        if (batch.getProcess().getTags() != null) {
            for (var tag : batch.getProcess().getTags()) {
                if ("hostname".equals(tag.getKey())) {
                    instanceName = tag.getVStr();

                    // host name has higher priority than ip, if found, exit the loop
                    break;
                } else if ("ip".equals(tag.getKey())) {
                    instanceName = tag.getVStr();
                }
            }
        }

        return new ApplicationInstance(appName, instanceName);
    }
}
