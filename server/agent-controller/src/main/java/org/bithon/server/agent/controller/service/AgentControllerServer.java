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

package org.bithon.server.agent.controller.service;

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.brpc.channel.BrpcServer;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.agent.controller.config.AgentControllerConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 7:37 下午
 */
@Slf4j
@Service
@ConditionalOnProperty(value = "bithon.agent-controller.enabled", havingValue = "true")
public class AgentControllerServer implements SmartLifecycle {

    static {
        // Make sure the underlying netty use JDK direct memory region so that the memory can be tracked
        System.setProperty("org.bithon.shaded.io.netty.maxDirectMemory", "0");
    }

    private final BrpcServer brpcServer;
    private final int port;
    private boolean isRunning = false;

    private final AgentSettingLoader loader;

    public AgentControllerServer(AgentSettingLoader loader, Environment env) {
        AgentControllerConfig config = Binder.get(env).bind("bithon.agent-controller", AgentControllerConfig.class).get();
        Preconditions.checkIfTrue(config.getPort() > 1000 && config.getPort() < 65535, "The port of bithon.agent-controller property must be in the range of [1000, 65535)");

        this.port = config.getPort();
        this.brpcServer = new BrpcServer("ctrl");
        this.brpcServer.bindService(new AgentSettingFetcher(loader));

        this.loader = loader;
    }

    public BrpcServer getBrpcServer() {
        Preconditions.checkNotNull(this.brpcServer, "The controller server has not started yet.");
        return this.brpcServer;
    }

    @Override
    public void start() {
        log.info("Starting Agent controller at port {}", this.port);
        this.loader.start();

        this.brpcServer.start(this.port);
        this.isRunning = true;
    }

    @Override
    public void stop() {
        log.info("Stopping Agent controller at port {}", this.port);
        this.brpcServer.close();

        this.loader.stop();
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
