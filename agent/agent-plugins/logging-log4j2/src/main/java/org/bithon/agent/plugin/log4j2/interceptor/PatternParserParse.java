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

package org.bithon.agent.plugin.log4j2.interceptor;

import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.InterceptionDecision;
import org.bithon.agent.core.logging.LogPatternInjector;

import java.util.List;

/**
 * {@link org.apache.logging.log4j.core.pattern.PatternParser#parse(String, List, List, boolean, boolean, boolean)}
 * <p>
 * automatically inject trace id into user's log pattern
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/23 5:00 下午
 */
public class PatternParserParse extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) {

        aopContext.getArgs()[0] = LogPatternInjector.injectTracePattern(aopContext.getArgAs(0));

        return InterceptionDecision.SKIP_LEAVE;
    }
}
