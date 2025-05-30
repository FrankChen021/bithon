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

package org.bithon.component.commons.logging.adaptor.log4j;

import org.apache.log4j.Logger;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.ILogAdaptorFactory;
import org.bithon.component.commons.logging.LoggerConfiguration;
import org.bithon.component.commons.logging.LoggingLevel;

import java.util.List;

public class Log4jLoggerFactory implements ILogAdaptorFactory {

    @Override
    public ILogAdaptor newLogger(String name) {
        return new Log4jLogAdaptor(Logger.getLogger(name));
    }

    @Override
    public List<LoggerConfiguration> getLoggerConfigurations() {
        throw new UnsupportedOperationException("log4j v1 is not supported");
    }

    @Override
    public void setLoggerConfiguration(String loggerName, LoggingLevel level) {
        throw new UnsupportedOperationException("log4j v1 is not supported");
    }
}
