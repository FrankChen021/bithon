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

package org.bithon.agent.plugin.apache.druid;

import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;
import org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin;
import org.bithon.shaded.net.bytebuddy.description.modifier.Visibility;

import java.util.Arrays;
import java.util.List;

import static org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptorBuilder.forClass;

/**
 * @author frankchen
 * @date 2022-01-04 18:35
 */
public class ApacheDruidPlugin implements IPlugin {

    @Override
    public List<InterceptorDescriptor> getInterceptors() {
        return Arrays.asList(
            forClass("org.apache.druid.sql.http.SqlResource")
                .onMethod("doPost")
                .interceptedBy("org.bithon.agent.plugin.apache.druid.interceptor.SqlResource$DoPost")
                .build(),

            forClass("org.apache.druid.server.QueryLifecycle")
                .onMethod("initialize")
                .interceptedBy("org.bithon.agent.plugin.apache.druid.interceptor.QueryLifecycle$Initialize")
                .build(),

            // Since Druid 24
            forClass("org.apache.druid.rpc.ServiceClientImpl")
                .onMethod("asyncRequest")
                .andVisibility(Visibility.PUBLIC)
                .andArgs(0, "org.apache.druid.rpc.RequestBuilder")
                .interceptedBy("org.bithon.agent.plugin.apache.druid.interceptor.ServiceClientImpl$AsyncRequest")
                .build()
        );
    }
}
