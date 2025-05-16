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

package org.bithon.agent.plugin.httpclient.apache.httpcomponents5.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;

import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicLong;

/**
 * {@link org.apache.hc.core5.http.impl.BasicHttpTransportMetrics#getBytesTransferred()}
 *
 * @author frank.chen021@outlook.com
 * @date 2024/4/27 13:22
 */
public class BasicHttpTransportMetrics$GetBytesTransferred extends AfterInterceptor {

    private Field bytesTransferredField;

    @Override
    public void after(AopContext aopContext) throws Exception {
        if (bytesTransferredField == null) {
            bytesTransferredField = aopContext.getTargetClass().getDeclaredField("bytesTransferred");
            bytesTransferredField.setAccessible(true);
        }

        // Reset the bytesTransferred field to 0 after each reading
        bytesTransferredField.set(aopContext.getTarget(), new AtomicLong());
    }
}
