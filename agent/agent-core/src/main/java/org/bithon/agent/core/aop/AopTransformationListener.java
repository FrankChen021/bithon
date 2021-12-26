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

package org.bithon.agent.core.aop;

import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.utility.JavaModule;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.util.Locale;

/**
 * @author Frank Chen
 * @date 26/12/21 1:50 PM
 */
public class AopTransformationListener extends AgentBuilder.Listener.Adapter {
    protected static final Logger log = LoggerFactory.getLogger(AopTransformationListener.class);

    @Override
    public void onError(String s, ClassLoader classLoader, JavaModule javaModule, boolean b, Throwable throwable) {
        log.error(String.format(Locale.ENGLISH, "Failed to transform %s", s), throwable);
    }
}
