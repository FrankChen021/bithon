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

package com.sbss.bithon.agent.core.config;

import java.util.Map;

/**
 * @author frankchen
 * @date 2020-12-31 22:18:18
 */
public class AgentConfig {
    private boolean traceEnabled = true;
    private BootstrapConfig bootstrap;
    private Map<String, DispatcherConfig> dispatchers;

    public boolean isTraceEnabled() {
        return traceEnabled;
    }

    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    public BootstrapConfig getBootstrap() {
        return bootstrap;
    }

    public void setBootstrap(BootstrapConfig bootstrap) {
        this.bootstrap = bootstrap;
    }

    public Map<String, DispatcherConfig> getDispatchers() {
        return dispatchers;
    }

    public void setDispatchers(Map<String, DispatcherConfig> dispatchers) {
        this.dispatchers = dispatchers;
    }
}
