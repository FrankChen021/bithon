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

package org.bithon.agent.config;

import org.bithon.agent.configuration.validation.GreaterThan;
import org.bithon.agent.configuration.validation.NotBlank;
import org.bithon.component.commons.utils.HumanReadableNumber;

/**
 * @author frankchen
 */
public class RpcClientConfig {

    /**
     * The factor class that creates the RPC object.
     * Example:
     * IAgentControllerFactory
     * IMessageChannelFactory
     */
    @NotBlank(message = "factory should not be blank.")
    private String factory;

    /**
     * how long a connection can be kept. in milliseconds
     */
    private int connectionLifeTime;

    /**
     * The timeout value in millisecond for establishing a connection to remote server.
     */
    @GreaterThan(value = 0)
    private int connectionTimeout = 2000;

    /**
     * The default value inside Netty is 32KiB, See WriteBufferWaterMark
     */
    @GreaterThan(value = 16 * 1024)
    private HumanReadableNumber lowWaterMark = HumanReadableNumber.of("32KiB");

    /**
     * The default value inside Netty is 64KiB, See WriteBufferWaterMark
     */
    @GreaterThan(value = 16 * 1024)
    private HumanReadableNumber highWaterMark = HumanReadableNumber.of("64KiB");

    public int getConnectionLifeTime() {
        return connectionLifeTime;
    }

    public void setConnectionLifeTime(int connectionLifeTime) {
        this.connectionLifeTime = connectionLifeTime;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public HumanReadableNumber getLowWaterMark() {
        return lowWaterMark;
    }

    public void setLowWaterMark(HumanReadableNumber lowWaterMark) {
        this.lowWaterMark = lowWaterMark;
    }

    public HumanReadableNumber getHighWaterMark() {
        return highWaterMark;
    }

    public void setHighWaterMark(HumanReadableNumber highWaterMark) {
        this.highWaterMark = highWaterMark;
    }
}
