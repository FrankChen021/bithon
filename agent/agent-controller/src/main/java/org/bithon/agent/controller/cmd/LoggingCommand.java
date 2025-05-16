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

package org.bithon.agent.controller.cmd;


import org.bithon.agent.rpc.brpc.cmd.ILoggingCommand;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.logging.LoggingLevel;

import java.util.Collections;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 16/4/25 8:30 pm
 */
public class LoggingCommand implements ILoggingCommand, IAgentCommand {
    @Override
    public List<LoggerConfiguration> getLoggers() {
        return LoggerFactory.getLogAdaptorFactory().getLoggerConfigurations();
    }

    @Override
    public List<Integer> setLogger(String name, LoggingLevel level) {
        LoggerFactory.getLogAdaptorFactory().setLoggerConfiguration(name, level);
        return Collections.singletonList(1);
    }
}
