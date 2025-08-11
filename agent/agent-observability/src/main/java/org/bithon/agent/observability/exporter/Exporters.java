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

package org.bithon.agent.observability.exporter;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.component.commons.logging.LoggerFactory;
import org.bithon.component.commons.utils.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/21 11:02 下午
 */
public class Exporters {
    /**
     * the name MUST correspond to the name of methods such as {@link IMessageExporterFactory#createMetricExporter(ExporterConfig)}
     */
    public static final String EXPORTER_NAME_METRIC = "metric";
    public static final String EXPORTER_NAME_TRACING = "tracing";
    public static final String EXPORTER_NAME_EVENT = "event";

    private static final Map<String, Exporter> EXPORTERS = new HashMap<>();

    public static Collection<Exporter> getAllDispatcher() {
        synchronized (EXPORTERS) {
            return new ArrayList<>(EXPORTERS.values());
        }
    }

    public static Exporter getOrCreate(String exporterName) {
        ExporterConfig config = ConfigurationManager.getInstance()
                                                    .getConfig("exporters." + exporterName, ExporterConfig.class, true);
        if (config == null) {
            return null;
        }
        Exporter exporter = EXPORTERS.get(exporterName);
        if (exporter != null) {
            return exporter;
        }

        synchronized (EXPORTERS) {
            // double check
            exporter = EXPORTERS.get(exporterName);
            if (exporter != null) {
                return exporter;
            }

            return EXPORTERS.computeIfAbsent(exporterName, key -> {
                try {
                    return new Exporter(exporterName,
                                        AppInstance.getInstance(),
                                        config);
                } catch (Exception e) {
                    LoggerFactory.getLogger(Exporters.class)
                                 .error(StringUtils.format("Failed to create exporter [%s]. Data may not be exported correctly." + exporterName), e);
                    return null;
                }
            });
        }
    }
}
