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
import org.slf4j.Logger;
import org.slf4j.helpers.NOPLoggerFactory;
import org.slf4j.spi.LocationAwareLogger;

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
}
