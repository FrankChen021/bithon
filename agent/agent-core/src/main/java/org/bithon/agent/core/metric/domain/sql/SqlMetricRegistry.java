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

package org.bithon.agent.core.metric.domain.sql;

import org.bithon.agent.core.metric.collector.MetricRegistry;
import org.bithon.agent.core.metric.collector.MetricRegistryFactory;

import java.util.Collections;

/**
 * @author frankchen
 */
public class SqlMetricRegistry extends MetricRegistry<SQLMetrics> {

    public static final String NAME = "sql-metrics";

    protected SqlMetricRegistry() {
        super(NAME,
              Collections.singletonList("connectionString"),
              SQLMetrics.class,
              SQLMetrics::new,
              true);
    }

    public static SqlMetricRegistry get() {
        return MetricRegistryFactory.getOrCreateRegistry(NAME, SqlMetricRegistry::new);
    }

    public SQLMetrics getOrCreateMetrics(String connectionString) {
        return super.getOrCreateMetrics(connectionString);
    }
}
