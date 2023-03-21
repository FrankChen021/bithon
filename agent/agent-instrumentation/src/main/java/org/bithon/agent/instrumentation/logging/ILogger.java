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

package org.bithon.agent.instrumentation.logging;

/**
 * Since Aop, which is injected into bootstrap class loader, depends on log,
 * and shaded.slf4j is not loaded by bootstrap class loader, we provide this class for Aop to log
 * <p>
 * NOTE: this class is injected into Bootstrap class loader
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/19 10:45 下午
 */
public interface ILogger {

    void warn(String messageFormat, Object... args);
    void warn(String message, Throwable e);
    void error(String message, Throwable e);
}
