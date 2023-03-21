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

package org.bithon.agent.instrumentation.aop;

/**
 * ALL transformed classes will be turned into as an instance of this interface
 *
 * @author frank.chen021@outlook.com
 * @date 2021/2/21 9:37 下午
 */
public interface IBithonObject {

    String INJECTED_FIELD_NAME = "_$BITHON_INJECTED_OBJECT$_";

    Object getInjectedObject();

    void setInjectedObject(Object value);
}
