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

package org.bithon.component.commons.logging.adaptor.slf4j;


import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.ILogAdaptorFactory;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;
import org.bithon.component.commons.logging.adaptor.log4j2.Log4j2LogAdaptorFactory;
import org.bithon.component.commons.logging.adaptor.logback.LogbackAdaptorFactory;
import org.bithon.component.commons.utils.StringUtils;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

import java.util.List;

public class Slf4jLogAdaptorFactory implements ILogAdaptorFactory {

    public Slf4jLogAdaptorFactory() {
        if (org.slf4j.LoggerFactory.getILoggerFactory() instanceof NOPLoggerFactory) {
            throw new NoClassDefFoundError("NOPLoggerFactory not supported");
        }
    }

    @Override
    public ILogAdaptor newLogger(String name) {
        Logger slf4jLogger = org.slf4j.LoggerFactory.getLogger(name);
        return slf4jLogger instanceof LocationAwareLogger
               ? new LocationAwareSlf4jLogAdaptor((LocationAwareLogger) slf4jLogger)
               : new Slf4JLogAdaptor(slf4jLogger);
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
        ILoggerFactory loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory();

        // DO NOT use instanceof which might trigger class loading, which in this case some logger factory classes may not be found
        switch (loggerFactory.getClass().getName()) {
            case "ch.qos.logback.classic.LoggerContext":
                return LogbackAdaptorFactory.getLoggerConfigurationList(loggerFactory);
            case "org.apache.logging.slf4j.Log4jLoggerFactory":
                return Log4j2LogAdaptorFactory.getLoggerConfigurationListImpl();
            default:
                throw new UnsupportedOperationException(StringUtils.format("Unsupported logger factory %s", loggerFactory.getClass().getName()));
        }
    }

    @Override
    public void setLoggerConfiguration(String loggerName, LoggingLevel level) {
        ILoggerFactory loggerFactory = org.slf4j.LoggerFactory.getILoggerFactory();

        // DO NOT use instanceof which might trigger class loading, which in this case some logger factory classes may not be found
        switch (loggerFactory.getClass().getName()) {
            case "ch.qos.logback.classic.LoggerContext":
                LogbackAdaptorFactory.setLogConfiguration(loggerFactory, loggerName, level);
                break;

            case "org.apache.logging.slf4j.Log4jLoggerFactory":
                Log4j2LogAdaptorFactory.setLogConfigurationImpl(loggerName, level);
                break;

            default:
                throw new UnsupportedOperationException(StringUtils.format("Unsupported logger factory %s", loggerFactory.getClass().getName()));
        }
    }
}
