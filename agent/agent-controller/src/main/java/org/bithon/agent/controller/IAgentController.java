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

package org.bithon.agent.controller;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 2:45 下午
 */
public interface IAgentController extends AutoCloseable {

    /**
     * An encapsulation of underlying configuration retrieval
     * So that higher level does not care which RPC is used to get the setting.
     */
    Map<String, String> getAgentConfiguration(String appName,
                                              String env,
                                              long lastModifiedSince);

    /**
     * Register a callback that once the underlying implementation changes, the getAgentConfiguration should be called immediately.
     */
    void refreshListener(Runnable runnable);

    void attachCommands(Object... commands);
}
