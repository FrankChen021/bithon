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

package org.bithon.component.commons.logging.adaptor.jdk;

import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.ILogAdaptorFactory;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.utils.StringUtils;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class JdkLoggerFactory implements ILogAdaptorFactory {

    private static final String ROOT_LOGGER_NAME = "ROOT";

    @Override
    public ILogAdaptor newLogger(String name) {
        return new JdkLogAdaptor(Logger.getLogger(name));
    }

    public List<LoggerConfiguration> getLoggerConfigurations() {
        List<LoggerConfiguration> configurationList = new ArrayList<>();
        Enumeration<String> names = LogManager.getLogManager().getLoggerNames();
        while (names.hasMoreElements()) {
            configurationList.add(getLoggerConfiguration(names.nextElement()));
        }
        configurationList.sort(LoggerConfiguration.Comparator.INSTANCE);
        return configurationList;
    }

    @Override
    public void setLoggerConfiguration(String loggerName, LoggingLevel level) {
        if (loggerName == null || ROOT_LOGGER_NAME.equals(loggerName)) {
            loggerName = "";
        }
        Logger logger = Logger.getLogger(loggerName);
        if (logger != null) {
            logger.setLevel(toNativeLevel(level));
        }
    }

    public LoggerConfiguration getLoggerConfiguration(String loggerName) {
        Logger logger = Logger.getLogger(loggerName);
        if (logger == null) {
            return null;
        }

        String name = (StringUtils.hasText(logger.getName()) ? logger.getName() : ROOT_LOGGER_NAME);
        return new LoggerConfiguration(name, toSystemLevel(logger.getLevel()), getEffectiveLevel(logger));
    }

    private LoggingLevel getEffectiveLevel(Logger root) {
        Logger logger = root;
        while (logger.getLevel() == null) {
            logger = logger.getParent();
        }
        return toSystemLevel(logger.getLevel());
    }

    private LoggingLevel toSystemLevel(Level level) {
        if (level == null) {
            return LoggingLevel.OFF;
        }
        if (level.intValue() >= Level.SEVERE.intValue()) {
            return LoggingLevel.ERROR;
        } else if (level.intValue() >= Level.WARNING.intValue()) {
            return LoggingLevel.WARN;
        } else if (level.intValue() >= Level.INFO.intValue()) {
            return LoggingLevel.INFO;
        } else if (level.intValue() >= Level.FINE.intValue()) {
            return LoggingLevel.DEBUG;
        } else {
            return LoggingLevel.TRACE;
        }
    }

    public static Level toNativeLevel(LoggingLevel level) {
        if (level == null) {
            return Level.OFF;
        }
        switch (level) {
            case FATAL:
            case ERROR:
                return Level.SEVERE;
            case WARN:
                return Level.WARNING;
            case INFO:
                return Level.INFO;
            case DEBUG:
                return Level.FINE;
            case TRACE:
                return Level.FINEST;
            default:
                return Level.OFF;
        }
    }
}
