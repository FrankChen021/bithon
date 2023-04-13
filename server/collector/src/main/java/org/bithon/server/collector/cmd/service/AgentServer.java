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

package org.bithon.server.collector.cmd.service;

import org.bithon.component.brpc.channel.BrpcServer;
import org.springframework.stereotype.Service;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/2 5:20 下午
 */
@Service
public class AgentServer {

    private BrpcServer brpcServer;

    public BrpcServer getBrpcServer() {
        return brpcServer;
    }

    public void setBrpcServer(BrpcServer brpcServer) {
        this.brpcServer = brpcServer;
    }
}
