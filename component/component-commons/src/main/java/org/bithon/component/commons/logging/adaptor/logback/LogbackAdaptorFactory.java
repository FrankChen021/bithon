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

package org.bithon.component.commons.logging.adaptor.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.ILogAdaptorFactory;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.impl.StaticLoggerBinder;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 26/12/21 6:35 PM
 */
public class LogbackAdaptorFactory implements ILogAdaptorFactory {
    private static final String ROOT_LOGGER_NAME = "ROOT";

    @Override
    public ILogAdaptor newLogger(String name) {
        return new LogbackLogAdaptor(StaticLoggerBinder.getSingleton().getLoggerFactory().getLogger(name));
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
        return getLoggerConfigurationList(getLoggerContext());
    }

    @Override
    public void setLoggerConfiguration(String loggerName, LoggingLevel level) {
        setLogConfiguration(getLoggerContext(), loggerName, level);
    }

    public static List<LoggerConfiguration> getLoggerConfigurationList(ILoggerFactory factory) {
        return getLoggerConfigurationList((LoggerContext) factory);
    }

    public static void setLogConfiguration(ILoggerFactory loggerFactory, String loggerName, LoggingLevel level) {
        setLogConfiguration((LoggerContext) loggerFactory, loggerName, level);
    }

    private static void setLogConfiguration(LoggerContext context, String loggerName, LoggingLevel level) {
        if (StringUtils.isEmpty(loggerName) || ROOT_LOGGER_NAME.equals(loggerName)) {
            loggerName = ROOT_LOGGER_NAME;
        }
        ch.qos.logback.classic.Logger logger = context.getLogger(loggerName);
        if (logger != null) {
            logger.setLevel(toNativeLevel(level));
        }
    }

    private static List<LoggerConfiguration> getLoggerConfigurationList(LoggerContext context) {
        return context.getLoggerList()
                      .stream()
                      .map(LogbackAdaptorFactory::toLoggerConfiguration)
                      .sorted(LoggerConfiguration.Comparator.INSTANCE)
                      .collect(Collectors.toList());
    }

    private static LoggerContext getLoggerContext() {
        ILoggerFactory factory = StaticLoggerBinder.getSingleton().getLoggerFactory();
        return (LoggerContext) factory;
    }

    private static LoggerConfiguration toLoggerConfiguration(Logger logger) {
        String name = logger.getName();
        if (!StringUtils.hasText(name) || Logger.ROOT_LOGGER_NAME.equals(name)) {
            name = Logger.ROOT_LOGGER_NAME;
        }
        return new LoggerConfiguration(name, toLoggingLevel(logger.getLevel()), toLoggingLevel(logger.getEffectiveLevel()));
    }

    private static LoggingLevel toLoggingLevel(Level level) {
        if (level == null) {
            return LoggingLevel.OFF;
        }
        switch (level.toInt()) {
            case Level.TRACE_INT:
                return LoggingLevel.TRACE;
            case Level.DEBUG_INT:
                return LoggingLevel.DEBUG;
            case Level.INFO_INT:
                return LoggingLevel.INFO;
            case Level.WARN_INT:
                return LoggingLevel.WARN;
            case Level.ERROR_INT:
                return LoggingLevel.ERROR;
            default:
                return LoggingLevel.OFF;
        }
    }

    private static Level toNativeLevel(LoggingLevel level) {
        if (level == null) {
            return Level.OFF;
        }
        switch (level) {
            case FATAL:
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
                return Level.OFF;
        }
    }
}
