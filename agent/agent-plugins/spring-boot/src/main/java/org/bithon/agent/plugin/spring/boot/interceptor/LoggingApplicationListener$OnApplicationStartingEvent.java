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

package org.bithon.agent.plugin.spring.boot.interceptor;

import org.bithon.agent.controller.AgentControllerService;
import org.bithon.agent.instrumentation.aop.context.AopContext;
import org.bithon.agent.instrumentation.aop.interceptor.AfterInterceptor;
import org.bithon.agent.rpc.brpc.cmd.ILoggingCommand;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.context.ApplicationEvent;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * SpringBoot 1.5+
 * {@link org.springframework.boot.logging.LoggingApplicationListener#onApplicationEvent(ApplicationEvent)}
 * <p>
 * SpringBoot 2.0+
 * {@link org.springframework.boot.context.logging.LoggingApplicationListener#onApplicationEvent(ApplicationEvent)}
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/2 15:36
 */
public class LoggingApplicationListener$OnApplicationStartingEvent extends AfterInterceptor {

    private ILoggingCommand springBootLoggingCommand;

    @Override
    public void after(AopContext aopContext) {
        if (springBootLoggingCommand != null) {
            return;
        }

        ApplicationEvent event = aopContext.getArgAs(0);
        if (event instanceof ApplicationEnvironmentPreparedEvent) {
            LoggingSystem loggingSystem = (LoggingSystem) ReflectionUtils.getFieldValue(aopContext.getTarget(), "loggingSystem");

            LoggerFactory.getLogger("Agent")
                         .info("Install logging command");

            springBootLoggingCommand = new SpringBootLoggingCommand(loggingSystem);
            AgentControllerService.getControllerInstance().attachCommands(springBootLoggingCommand);
        }
    }

    static class SpringBootLoggingCommand implements ILoggingCommand {
        private final LoggingSystem loggingSystem;

        private final Map<LogLevel, LoggingLevel> toLoggingLevel;
        private final Map<LoggingLevel, LogLevel> toLocalLevel;

        public SpringBootLoggingCommand(LoggingSystem loggingSystem) {
            this.loggingSystem = loggingSystem;

            toLoggingLevel = new HashMap<LogLevel, LoggingLevel>() {
                @Override
                public LoggingLevel get(Object key) {
                    if (key == null) {
                        return null;
                    }

                    LoggingLevel level = super.get(key);
                    if (level == null) {
                        throw new UnsupportedOperationException("Not supported local level" + key);
                    }
                    return level;
                }
            };
            toLoggingLevel.put(LogLevel.FATAL, LoggingLevel.FATAL);
            toLoggingLevel.put(LogLevel.ERROR, LoggingLevel.ERROR);
            toLoggingLevel.put(LogLevel.WARN, LoggingLevel.WARN);
            toLoggingLevel.put(LogLevel.INFO, LoggingLevel.INFO);
            toLoggingLevel.put(LogLevel.DEBUG, LoggingLevel.DEBUG);
            toLoggingLevel.put(LogLevel.TRACE, LoggingLevel.TRACE);
            toLoggingLevel.put(LogLevel.OFF, LoggingLevel.OFF);

            toLocalLevel = new HashMap<LoggingLevel, LogLevel>() {
                @Override
                public LogLevel get(Object key) {
                    if (key == null) {
                        return null;
                    }
                    LogLevel level = super.get(key);
                    if (level == null) {
                        throw new UnsupportedOperationException("Not supported logging level" + key);
                    }
                    return level;
                }
            };
            toLocalLevel.put(LoggingLevel.FATAL, LogLevel.FATAL);
            toLocalLevel.put(LoggingLevel.ERROR, LogLevel.ERROR);
            toLocalLevel.put(LoggingLevel.WARN, LogLevel.WARN);
            toLocalLevel.put(LoggingLevel.INFO, LogLevel.INFO);
            toLocalLevel.put(LoggingLevel.DEBUG, LogLevel.DEBUG);
            toLocalLevel.put(LoggingLevel.TRACE, LogLevel.TRACE);
            toLocalLevel.put(LoggingLevel.OFF, LogLevel.OFF);
        }

        @Override
        public List<LoggerConfiguration> getLoggers() {
            return loggingSystem.getLoggerConfigurations()
                                .stream()
                                .map((cfg) -> {
                                    LoggerConfiguration configuration = new LoggerConfiguration();
                                    configuration.name = (cfg.getName());
                                    configuration.level = (toLoggingLevel.get(cfg.getConfiguredLevel()));
                                    configuration.effectiveLevel = (toLoggingLevel.get(cfg.getEffectiveLevel()));
                                    return configuration;
                                }).collect(Collectors.toList());
        }

        @Override
        public List<Integer> setLogger(String name, LoggingLevel level) {
            loggingSystem.setLogLevel(name, toLocalLevel.get(level));
            return Collections.singletonList(1);
        }
    }
}
