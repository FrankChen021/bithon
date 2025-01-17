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


import org.bithon.agent.observability.context.AppInstance;
import org.bithon.agent.observability.exporter.config.ExporterConfig;
import org.bithon.agent.observability.exporter.task.BlockingQueue;
import org.bithon.agent.observability.exporter.task.ExportTask;
import org.bithon.agent.observability.exporter.task.IMessageQueue;
import org.bithon.component.commons.logging.ILogAdaptor;
import org.bithon.component.commons.logging.LoggerFactory;

import java.lang.reflect.Method;
import java.util.function.Consumer;

/**
 * @author frankchen
 */
public class Exporter {
    private static final ILogAdaptor LOG = LoggerFactory.getLogger(Exporter.class);

    private final String exporterName;
    private final IMessageConverter messageConverter;
    private final IMessageExporter messageExporter;
    private final ExporterConfig exporterConfig;
    private int appPort;
    private ExportTask task;

    Exporter(String exporterName,
             AppInstance appInstance,
             ExporterConfig exporterConfig) throws Exception {
        this.exporterName = exporterName;
        this.exporterConfig = exporterConfig;

        //
        // create exporter instance from configuration
        //
        IMessageExporterFactory factory = createDispatcherFactory(exporterConfig);
        Method createMethod = IMessageExporterFactory.class.getMethod("create" + capitalize(exporterName) + "Exporter",
                                                                      ExporterConfig.class);
        this.messageExporter = (IMessageExporter) createMethod.invoke(factory, exporterConfig);
        this.messageConverter = factory.createMessageConverter();

        if (appInstance.getPort() == 0) {
            appInstance.addListener(this::startTask);
        }
        startTask(appInstance.getPort());
    }

    public boolean isReady() {
        return task != null && task.canAccept();
    }

    public void onReady(Consumer<Exporter> listener) {
        AppInstance.getInstance().addListener(port -> listener.accept(this));
    }

    public IMessageConverter getMessageConverter() {
        return this.messageConverter;
    }

    public void send(Object message) {
        if (task != null && message != null) {
            task.accept(message);
        }
    }

    private String capitalize(String s) {
        if (Character.isLowerCase(s.charAt(0))) {
            return (char) (s.charAt(0) - 'a' + 'A') + s.substring(1);
        } else {
            return s;
        }
    }

    private IMessageExporterFactory createDispatcherFactory(ExporterConfig config) throws Exception {
        return (IMessageExporterFactory) Class.forName(config.getClient().getFactory())
                                              .getDeclaredConstructor()
                                              .newInstance();
    }

    private synchronized void startTask(int port) {
        if (appPort != 0 || port == 0) {
            return;
        }
        this.appPort = port;

        LOG.info("Application port updated to {}, {} will soon be at work",
                 port,
                 this.exporterName);

        task = new ExportTask(exporterName,
                              createQueue(exporterConfig),
                              exporterConfig,
                              messageExporter::export);
    }

    private IMessageQueue createQueue(ExporterConfig config) {
        return new BlockingQueue(config.getQueueSize());
    }

    public void shutdown() {
        LOG.info("Shutting down exporter task [{}]...", exporterName);
        if (task != null) {
            task.stop();
        }

        // stop underlying message exporter
        LOG.info("Closing message exporter [{}]...", exporterName);
        try {
            messageExporter.close();
        } catch (Exception ignored) {
        }
    }
}
