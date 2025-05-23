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
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.apache.logging.log4j.core.impl.Log4jContextFactory;
import org.apache.logging.log4j.core.selector.ContextSelector;
import org.apache.logging.log4j.spi.LoggerContextFactory;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.ILogAdaptorFactory;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.apache.logging.log4j.LogManager.ROOT_LOGGER_NAME;

public final class Log4j2LogAdaptorFactory implements ILogAdaptorFactory {

    @Override
    public ILogAdaptor newLogger(String name) {
        return new Log4j2LogAdaptor(LogManager.getLogger(name));
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
        return getLoggerConfigurationListImpl();
    }

    @Override
    public void setLoggerConfiguration(String loggerName, LoggingLevel level) {
        setLogConfigurationImpl(loggerName, level);
    }

    public static void setLogConfigurationImpl(String loggerName, LoggingLevel level) {
        if (!StringUtils.hasText(loggerName) || ROOT_LOGGER_NAME.equals(loggerName)) {
            loggerName = LogManager.ROOT_LOGGER_NAME;
        }

        boolean updated = false;
        List<LoggerContext> contexts = getLoggerContexts();
        for (LoggerContext context : contexts) {
            LoggerConfig loggerConfig = context.getConfiguration()
                                               .getLoggers()
                                               .get(loggerName);
            if (loggerConfig != null) {
                loggerConfig.setLevel(toNativeLevel(level));
                context.updateLoggers();
                updated = true;
            }
        }

        if (updated) {
            return;
        }

        // Set logger in all contexts if the given logger does not exist in ALL contexts
        for (LoggerContext context : getLoggerContexts()) {
            LoggerConfig loggerConfig = context.getConfiguration()
                                               .getLoggers()
                                               .get(loggerName);
            if (loggerConfig == null) {
                loggerConfig = new LoggerConfig(loggerName, toNativeLevel(level), true);
                context.getConfiguration().addLogger(loggerName, loggerConfig);
            } else {
                loggerConfig.setLevel(toNativeLevel(level));
            }
            context.updateLoggers();
        }
    }

    public static List<LoggerConfiguration> getLoggerConfigurationListImpl() {
        return getLoggerContexts().stream()
                                  .flatMap((context) -> context.getLoggers().stream())
                                  .map(Log4j2LogAdaptorFactory::toLoggerConfiguration)
                                  // Merge loggers with the SAME name, return the logger with a higher level
                                  .collect(Collectors.toMap((logger) -> logger.name, Function.identity(), (lhs, rhs) -> lhs.level.intLevel() > rhs.level.intLevel() ? lhs : rhs))
                                  .values()
                                  .stream()
                                  .sorted(LoggerConfiguration.Comparator.INSTANCE)
                                  .collect(Collectors.toList());
    }

    private static List<LoggerContext> getLoggerContexts() {
        LoggerContextFactory contextFactory = LogManager.getFactory();
        if (contextFactory instanceof Log4jContextFactory) {
            Log4jContextFactory factory = (Log4jContextFactory) LogManager.getFactory();
            ContextSelector selector = factory.getSelector();
            return selector.getLoggerContexts();
        } else {
            LoggerContext context = (LoggerContext) LogManager.getContext(ClassLoader.getSystemClassLoader(), false);
            return Collections.singletonList(context);
        }
    }

    private static LoggerConfiguration toLoggerConfiguration(Logger logger) {
        LoggingLevel level = toLoggingLevel(logger.getLevel());
        String name = logger.getName();
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
