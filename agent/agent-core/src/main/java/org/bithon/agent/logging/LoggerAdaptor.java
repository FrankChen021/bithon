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

package org.bithon.agent.logging;

import org.bithon.agent.instrumentation.logging.ILogger;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.component.commons.logging.ILogAdaptor;

/**
 * created via reflection from bootstrap aop instances which are loaded by bootstrap class loader
 * see {@link LoggerFactory}
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 10:49 下午
 */
public class LoggerAdaptor implements ILogger {

    private final ILogAdaptor log;

    private LoggerAdaptor(Class<?> logClass) {
        this.log = org.bithon.component.commons.logging.LoggerFactory.getLogger(logClass);
    }

    public static ILogger getLogger(Class<?> clazz) {
        return new LoggerAdaptor(clazz);
    }

    @Override
    public void info(String messageFormat, Object... args) {
        log.info(messageFormat, args);
    }

    @Override
    public void warn(String messageFormat, Object... args) {
        log.warn(messageFormat, args);
    }

    @Override
    public void warn(String message, Throwable e) {
        log.warn(message, e);
    }

    @Override
    public void error(String message, Throwable e) {
        log.error(message, e);
    }

    @Override
    public void error(String messageFormat, Object... args) {
        log.error(messageFormat, args);
    }
}
