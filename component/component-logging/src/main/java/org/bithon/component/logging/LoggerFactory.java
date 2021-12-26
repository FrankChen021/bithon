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

package org.bithon.component.logging;

import org.bithon.component.logging.adaptor.jdk.JdkLoggerFactory;
import org.bithon.component.logging.adaptor.log4j.Log4jLoggerFactory;
import org.bithon.component.logging.adaptor.log4j2.Log4j2LogAdaptorFactory;
import org.bithon.component.logging.adaptor.logback.LogbackAdaptorFactory;
import org.bithon.component.logging.adaptor.slf4j.Slf4jLogAdaptorFactory;


public class LoggerFactory {

    private static volatile ILogAdaptorFactory adaptorFactory;

    private static ILogAdaptorFactory getLogAdaptorFactory() {
        if (adaptorFactory != null) {
            return adaptorFactory;
        }

        synchronized (LoggerFactory.class) {
            if (adaptorFactory != null) {
                return adaptorFactory;
            }

            Class<?>[] factoryClassList = new Class[]{
                Slf4jLogAdaptorFactory.class,
                LogbackAdaptorFactory.class,
                Log4j2LogAdaptorFactory.class,
                Log4jLoggerFactory.class,
                JdkLoggerFactory.class
            };
            for (Class<?> factoryClass : factoryClassList) {
                try {
                    ILogAdaptorFactory factory = (ILogAdaptorFactory) factoryClass.newInstance();
                    factory.newLogger("default").debug("Using [{}] as logging framework", factory.getClass().getSimpleName());
                    LoggerFactory.adaptorFactory = factory;
                    break;
                } catch (InstantiationException | IllegalAccessException ignored) {
                }
            }
        }
        return adaptorFactory;
    }

    public static ILogAdaptor getLogger(Class<?> clazz) {
        return getLogger(clazz.getName());
    }

    public static ILogAdaptor getLogger(String name) {
        return getLogAdaptorFactory().newLogger(name);
    }
}
