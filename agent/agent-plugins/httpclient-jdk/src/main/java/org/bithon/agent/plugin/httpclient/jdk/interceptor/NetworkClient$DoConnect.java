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

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketImpl;

/**
 * {@link sun.net.NetworkClient#doConnect(String, int)}
 *
 * @author frank.chen021@outlook.com
 * @date 2022/8/21 15:39
 */
public class NetworkClient$DoConnect extends AbstractInterceptor {

    @Override
    public void onMethodLeave(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        if (!(aopContext.getTarget() instanceof IBithonObject)) {
            // NetworkClient has many subclasses, some of which are not instrumented
            return;
        }

        // doConnect is called inside construction of HttpClient object
        // we need to initialize context object here
        // but in case code change in JDK, we still check if context object is initialized
        IBithonObject bithonObject = (IBithonObject) aopContext.getTarget();
        if (bithonObject.getInjectedObject() == null) {
            bithonObject.setInjectedObject(new HttpClientContext());
        }

        final HttpClientContext clientContext = (HttpClientContext) bithonObject.getInjectedObject();

        Object socket = aopContext.getReturning();
        if (socket instanceof IBithonObject) {
            ((IBithonObject) socket).setInjectedObject(clientContext);
        }
    }

    static class SocketProxy extends Socket {

        private final HttpClientContext clientContext;

        SocketProxy(SocketImpl impl, HttpClientContext clientContext) throws SocketException {
            super(impl);
            this.clientContext = clientContext;
        }

        @Override
        public InputStream getInputStream() throws IOException {
            InputStream is = super.getInputStream();
            return new FilterInputStream(is) {
                @Override
                public int read(byte[] b, int off, int len) throws IOException {
                    int size = super.read(b, off, len);
                    clientContext.getReceiveBytes().update(size);
                    return size;
                }
            };
        }

        @Override
        public OutputStream getOutputStream() throws IOException {
            OutputStream os = super.getOutputStream();
            return new FilterOutputStream(os) {
                @Override
                public void write(byte[] b, int off, int len) throws IOException {
                    super.write(b, off, len);
                    clientContext.getSentBytes().update(len);
                }
            };
        }
    }
}
