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

package org.bithon.agent.core.metric.domain.web;

import org.bithon.agent.core.metric.collector.MetricRegistry;

import java.util.Collections;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/14 9:30 下午
 */
public class WebServerMetricRegistry extends MetricRegistry<WebServerMetrics> {

    public static final String NAME = "web-server-metrics";

    public WebServerMetricRegistry() {
        super(NAME,
              Collections.singletonList("type"),
              WebServerMetrics.class,
              null,
              false);
    }
}
