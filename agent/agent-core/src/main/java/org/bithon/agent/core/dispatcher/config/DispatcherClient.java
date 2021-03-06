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

package org.bithon.agent.core.dispatcher.config;

/**
 * @author frankchen
 */
public class DispatcherClient {

    /**
     * must be sub-class of {@link org.bithon.agent.core.dispatcher.channel.IMessageChannelFactory}
     */
    private String factory;

    /**
     * how long a connection can be kept. in milliseconds
     */
    private int maxLifeTime;

    public int getMaxLifeTime() {
        return maxLifeTime;
    }

    public void setMaxLifeTime(int maxLifeTime) {
        this.maxLifeTime = maxLifeTime;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }
}
