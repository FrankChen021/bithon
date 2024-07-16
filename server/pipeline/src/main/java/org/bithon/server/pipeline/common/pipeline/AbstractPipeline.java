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

package org.bithon.server.pipeline.common.pipeline;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.pipeline.common.transformer.ExceptionSafeTransformer;
import org.bithon.server.pipeline.common.transformer.ITransformer;
import org.slf4j.Logger;
import org.springframework.context.SmartLifecycle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
public abstract class AbstractPipeline<RECEIVER extends IReceiver, EXPORTER extends IExporter> implements SmartLifecycle {

    protected final ObjectMapper objectMapper;
    protected PipelineConfig pipelineConfig;

    protected final List<RECEIVER> receivers;
    protected final List<ITransformer> processors;
    protected final List<EXPORTER> exporters = new ArrayList<>();
    private boolean isRunning = false;

    public AbstractPipeline(Class<RECEIVER> receiverClass,
                            Class<EXPORTER> exporterClass,
                            PipelineConfig pipelineConfig,
                            ObjectMapper objectMapper) {
        this.pipelineConfig = pipelineConfig;
        this.receivers = createReceivers(pipelineConfig, objectMapper, receiverClass);
        this.processors = createProcessors(pipelineConfig, objectMapper);
        this.objectMapper = objectMapper;

        initializeExportersFromConfig(pipelineConfig, objectMapper, exporterClass);
    }

    private <T> T createObject(Class<T> clazz, ObjectMapper objectMapper, Object configuration) throws IOException {
        return objectMapper.readValue(objectMapper.writeValueAsBytes(configuration), clazz);
    }

    private List<RECEIVER> createReceivers(PipelineConfig pipelineConfig,
                                           ObjectMapper objectMapper,
                                           Class<RECEIVER> receiverClass) {
        if (!pipelineConfig.isEnabled()) {
            return Collections.emptyList();
        }

        Preconditions.checkIfTrue(!CollectionUtils.isEmpty(pipelineConfig.getReceivers()),
                                  "The pipeline processing is enabled, but no receivers defined.");

        return pipelineConfig.getReceivers()
                             .stream()
                             .map((receiverConfig) -> {
                                 try {
                                     return createObject(receiverClass, objectMapper, receiverConfig);
                                 } catch (IOException e) {
                                     throw new RuntimeException(e);
                                 }
                             }).collect(Collectors.toList());
    }

    private List<ITransformer> createProcessors(PipelineConfig pipelineConfig,
                                                ObjectMapper objectMapper) {
        if (!pipelineConfig.isEnabled()) {
            return new ArrayList<>();
        }

        List<ITransformer> transformers = new ArrayList<>();
        if (CollectionUtils.isEmpty(pipelineConfig.getProcessors())) {
            return transformers;
        }

        for (Object transform : pipelineConfig.getProcessors()) {
            try {
                transformers.add(new ExceptionSafeTransformer(createObject(ITransformer.class, objectMapper, transform)));
            } catch (IOException e) {
                throw new RuntimeException("Failed to initialize transformer", e);
            }
        }
        return transformers;
    }

    private void initializeExportersFromConfig(PipelineConfig pipelineConfig,
                                               ObjectMapper objectMapper,
                                               Class<EXPORTER> exporterClass) {
        if (!pipelineConfig.isEnabled()) {
            return;
        }

        Preconditions.checkIfTrue(!CollectionUtils.isEmpty(pipelineConfig.getExporters()),
                                  "The pipeline processing is enabled, but no exporter defined.");

        // No use of stream API because we need to return a modifiable list
        for (Object exporterConfig : pipelineConfig.getExporters()) {
            try {
                link(createObject(exporterClass, objectMapper, exporterConfig));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public EXPORTER link(EXPORTER exporter) {
        getLogger().info("Add exporter [{}] to {}", exporter, this.getClass().getSimpleName());

        synchronized (exporters) {
            exporters.add(exporter);
        }
        return exporter;
    }

    public EXPORTER unlink(EXPORTER exporter) {
        getLogger().info("Remove exporter [{}] from {}", exporter, this.getClass().getSimpleName());
        synchronized (exporters) {
            exporters.remove(exporter);
        }
        return exporter;
    }

    @Override
    public void start() {
        if (!this.pipelineConfig.isEnabled()) {
            getLogger().info("{} is not enabled.", this.getClass().getSimpleName());
            return;
        }

        this.registerProcessor();

        getLogger().info("Starting exporters of {}...", this.getClass().getSimpleName());
        for (IExporter exporter : this.exporters) {
            exporter.start();
        }

        getLogger().info("Starting receivers of {}...", this.getClass().getSimpleName());
        for (IReceiver receiver : this.receivers) {
            receiver.start();
        }

        this.isRunning = true;
    }

    protected abstract void registerProcessor();

    protected abstract Logger getLogger();

    @Override
    public void stop() {
        if (!this.pipelineConfig.isEnabled()) {
            return;
        }

        // Stop the receiver first
        getLogger().info("Stopping receivers of {}...", this.getClass().getSimpleName());
        for (IReceiver receiver : this.receivers) {
            receiver.stop();
        }

        getLogger().info("Stopping exporters of {}...", this.getClass().getSimpleName());
        for (IExporter exporter : this.exporters) {
            try {
                exporter.stop();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public boolean isRunning() {
        return isRunning;
    }
}
