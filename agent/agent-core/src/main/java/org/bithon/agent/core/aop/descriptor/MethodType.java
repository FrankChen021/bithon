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

package org.bithon.agent.core.aop.descriptor;

/**
 * An enum describe the type of intercepted methods
 * @author frank.chen021@outlook.com
 * @date 2021/2/20 9:42 下午
 */
public enum MethodType {

    CONSTRUCTOR,

    /**
     * for those which are not constructors
     */
    NON_CONSTRUCTOR,

    /**
     * replace the original method implementation
     * this only works for non-constructor methods
     */
    REPLACEMENT;
}
