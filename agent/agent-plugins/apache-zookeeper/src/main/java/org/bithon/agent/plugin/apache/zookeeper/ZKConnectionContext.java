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

package org.bithon.agent.plugin.apache.zookeeper;

import org.bithon.agent.instrumentation.aop.IBithonObject;

/**
 *
 * @author frank.chen021@outlook.com
 * @date 15/1/25 5:46 pm
 */
public class ZKConnectionContext {
    private final IBithonObject clientSocket;

    public ZKConnectionContext(Object clientSocket) {
        this.clientSocket = clientSocket instanceof IBithonObject ? (IBithonObject) clientSocket : null;
    }

    /**
     * The remote address is injected to the socket object in the {@link ClientCnxnSocket$Connect} after the 'connect'
     * method is called
     */
    public String getServerAddress() {
        return (String) clientSocket.getInjectedObject();
    }
}
