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

package org.bithon.agent.plugin.httpclient.jdk.interceptor;

import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * {@link java.net.Socket#getOutputStream()}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/8/21 16:57
 */
public class Socket$GetOutputStream extends AfterInterceptor {

    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        // this object is assigned in NetworkClient#doConnect
        HttpClientContext clientContext = aopContext.getInjectedOnTargetAs();
        if (clientContext == null) {
            return;
        }

        OutputStream os = aopContext.getReturningAs();

        aopContext.setReturning(new FilterOutputStream(os) {
            @Override
            public void write(byte[] b, int off, int len) throws IOException {
                // Call the target's write method directly instead of the super's because the super's implementation has very poor performance
                out.write(b, off, len);
                clientContext.getSentBytes().update(len);
            }
        });
    }
}
