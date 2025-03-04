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

package org.bithon.agent.controller.impl.brpc;

import org.bithon.agent.controller.AgentControllerConfig;
import org.bithon.agent.controller.IAgentController;
import org.bithon.agent.controller.IAgentControllerFactory;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 4:40 下午
 */
public class BrpcAgentControllerFactory implements IAgentControllerFactory {

    static {
        // Make sure the underlying netty use JDK direct memory region so that the memory can be tracked
        System.setProperty("org.bithon.shaded.io.netty.maxDirectMemory", "0");
    }

    @Override
    public IAgentController createController(AgentControllerConfig config) {
        return new BrpcAgentController(config);
    }
}
