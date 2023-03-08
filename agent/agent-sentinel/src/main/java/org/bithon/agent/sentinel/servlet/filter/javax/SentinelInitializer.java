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

package org.bithon.agent.sentinel.servlet.filter.javax;

import org.bithon.agent.sentinel.servlet.filter.SentinelListener;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import java.util.EnumSet;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/5 23:48
 */
public class SentinelInitializer implements ServletContainerInitializer {
    @Override
    public void onStartup(Set<Class<?>> c, ServletContext ctx) {
        ILogAdaptor log = LoggerFactory.getLogger(SentinelInitializer.class);
        try {
            ctx.addFilter("org.bithon.agent.sentinel", new JavaxServletFilter(new SentinelListener()))
               .addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
            log.info("Sentinel for tomcat installed");
        } catch (Exception e) {
            log.error("Exception occurred when initialize servlet context. sentinel may not be installed", e);
        }
    }
}
