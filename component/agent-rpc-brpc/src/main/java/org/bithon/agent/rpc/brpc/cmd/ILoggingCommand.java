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

package org.bithon.agent.rpc.brpc.cmd;

import org.bithon.component.brpc.BrpcService;
import org.bithon.component.brpc.message.serializer.Serializer;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/1 21:22
 */
@BrpcService(name = "agent.logging", serializer = Serializer.JSON_SMILE)
public interface ILoggingCommand {

    List<LoggerConfiguration> getLoggers();

    int setLogger(String name, LoggingLevel level);
}
