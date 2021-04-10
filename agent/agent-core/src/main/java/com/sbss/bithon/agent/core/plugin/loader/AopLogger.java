/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.plugin.loader;

import com.sbss.bithon.agent.boot.aop.IAopLogger;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

/**
 * created via reflection from bootstrap aop instances which are loaded by bootstrap class loader
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 10:49 下午
 */
public class AopLogger implements IAopLogger {

    private final Logger log;

    private AopLogger(Class<?> logClass) {
        this.log = LoggerFactory.getLogger(logClass);
    }

    public static IAopLogger getLogger(Class<?> clazz) {
        return new AopLogger(clazz);
    }

    @Override
    public void warn(String message, Throwable e) {
        log.warn(message, e);
    }

    @Override
    public void error(String message, Throwable e) {
        log.error(message, e);
    }
}
