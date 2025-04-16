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

package org.bithon.component.commons.logging.adaptor.log4j2;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.slf4j.Log4jLoggerFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.ILogAdaptorFactory;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.ReflectionUtils;
import org.bithon.component.commons.utils.StringUtils;
import org.slf4j.ILoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;

public final class Log4j2LogAdaptorFactory implements ILogAdaptorFactory {

    @Override
    public ILogAdaptor newLogger(String name) {
        return new Log4j2LogAdaptor(LogManager.getLogger(name));
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
        return getLoggerConfigurationList(getLoggerContext());
    }

    @Override
    public void setLoggerConfiguration(String loggerName, LoggingLevel level) {
        setLogConfiguration(getLoggerContext(), loggerName, level);
    }

    public static void setLogConfiguration(ILoggerFactory loggerFactory, String loggerName, LoggingLevel level) {
        LoggerContext context;
        try {
            Log4jLoggerFactory factory = (Log4jLoggerFactory) loggerFactory;
            context = ReflectionUtils.invoke(factory, "getContext");
        } catch (Exception e) {
            context = getLoggerContext();
        }

        setLogConfiguration(context, loggerName, level);
    }

    private static void setLogConfiguration(LoggerContext context, String loggerName, LoggingLevel level) {
        Level nativeLevel = toNativeLevel(level);

        if (!StringUtils.hasText(loggerName) || ROOT_LOGGER_NAME.equals(loggerName)) {
            loggerName = LogManager.ROOT_LOGGER_NAME;
        }

        LoggerConfig loggerConfig = context.getConfiguration()
                                           .getLoggers()
                                           .get(loggerName);
        if (loggerConfig == null) {
            loggerConfig = new LoggerConfig(loggerName, nativeLevel, true);
            context.getConfiguration().addLogger(loggerName, loggerConfig);
        } else {
            loggerConfig.setLevel(nativeLevel);
        }
        context.updateLoggers();
    }

    public static List<LoggerConfiguration> getLoggerConfigurationList(ILoggerFactory loggerFactory) {
        LoggerContext context;
        try {
            Log4jLoggerFactory factory = (Log4jLoggerFactory) loggerFactory;
            context = ReflectionUtils.invoke(factory, "getContext");
        } catch (Exception e) {
            context = getLoggerContext();
        }

        return getLoggerConfigurationList(context);
    }

    private static List<LoggerConfiguration> getLoggerConfigurationList(LoggerContext loggerContext) {
        return loggerContext.getConfiguration()
                            .getLoggers()
                            .values()
                            .stream()
                            .map(Log4j2LogAdaptorFactory::toLoggerConfiguration)
                            .sorted(LoggerConfiguration.Comparator.INSTANCE)
                            .collect(Collectors.toList());
    }

    private static LoggerContext getLoggerContext() {
        return (LoggerContext) org.apache.logging.log4j.LogManager.getContext(false);
    }

    private static LoggerConfiguration toLoggerConfiguration(LoggerConfig loggerConfig) {
        if (loggerConfig == null) {
            return null;
        }
        LoggingLevel level = toLoggingLevel(loggerConfig.getLevel());
        String name = loggerConfig.getName();
        if (!StringUtils.hasText(name) || ROOT_LOGGER_NAME.equals(name)) {
            name = ROOT_LOGGER_NAME;
        }
        return new LoggerConfiguration(name, level, level);
    }

    private static LoggingLevel toLoggingLevel(Level level) {
        switch (level.name()) {
            case "OFF":
                return LoggingLevel.OFF;
            case "FATAL":
                return LoggingLevel.FATAL;
            case "ERROR":
                return LoggingLevel.ERROR;
            case "WARN":
                return LoggingLevel.WARN;
            case "INFO":
                return LoggingLevel.INFO;
            case "DEBUG":
                return LoggingLevel.DEBUG;
            case "TRACE":
                return LoggingLevel.TRACE;
            default:
                throw new UnsupportedOperationException("Not supported logging level: " + level);
        }
    }

    public static Level toNativeLevel(LoggingLevel level) {
        if (level == null) {
            return Level.OFF;
        }
        switch (level) {
            case FATAL:
                return Level.FATAL;
            case ERROR:
                return Level.ERROR;
            case WARN:
                return Level.WARN;
            case INFO:
                return Level.INFO;
            case DEBUG:
                return Level.DEBUG;
            case TRACE:
                return Level.TRACE;
            default:
                throw new UnsupportedOperationException("Not supported logging level: " + level);
        }
    }
}
