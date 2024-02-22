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

package org.bithon.agent.instrumentation.bytecode;

/**
 * The interface on the proxied object.
 * See {@link ProxyClassGenerator} for more.
 * NOTE: DON'T CHANGE the method names because they're used as STRING in the {@link ProxyClassGenerator}
 *
 * @author frank.chen021@outlook.com
 * @date 2023/1/7 16:12
 */
public interface IProxyObject {
    Class<?> getProxyClass();

    /**
     * Allow dynamically changing the delegated object
     */
    void setProxyObject(Object val);
}
