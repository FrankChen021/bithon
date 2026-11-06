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

import org.bithon.agent.observability.exporter.task.ExportTask;
import org.bithon.agent.starter.IAgentShutdownListener;

import java.util.Collection;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/17 00:43
 */
public class ExporterShutdown implements IAgentShutdownListener {

    @Override
    public int getOrder() {
        return Integer.MIN_VALUE;
    }

    @Override
    public void shutdown() {
        // All exporters share one deadline and flush in parallel,
        // so that the shutdown takes at most one timeout even if the server is unreachable
        long deadline = System.currentTimeMillis() + ExportTask.DEFAULT_STOP_TIMEOUT_MS;

        Collection<Exporter> exporters = Exporters.getAllDispatcher();
        for (Exporter exporter : exporters) {
            exporter.shutdown();
        }
        for (Exporter exporter : exporters) {
            exporter.awaitTermination(deadline);
        }
    }
}
