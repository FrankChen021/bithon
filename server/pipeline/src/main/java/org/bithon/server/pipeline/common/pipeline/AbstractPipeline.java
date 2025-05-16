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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.bithon.component.commons.utils.CollectionUtils;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.commons.spring.EnvironmentBinder;
import org.bithon.server.pipeline.common.transformer.ExceptionSafeTransformer;
import org.bithon.server.pipeline.common.transformer.ITransformer;
import org.slf4j.Logger;
import org.springframework.beans.BeansException;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.SmartLifecycle;
import org.springframework.core.env.ConfigurableEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * NOTE: Subclasses must be instantiated as Spring beans
 *
 * @author frank.chen021@outlook.com
 * @date 12/4/22 11:38 AM
 */
public abstract class AbstractPipeline<RECEIVER extends IReceiver, EXPORTER extends IExporter>
    implements SmartLifecycle, ApplicationListener<EnvironmentChangeEvent>, ApplicationContextAware {

    private ApplicationContext applicationContext;
    protected final ObjectMapper objectMapper;

    protected PipelineConfig pipelineConfig;

    protected final List<RECEIVER> receivers;
    private List<ITransformer> processors;
    protected final List<EXPORTER> exporters = new ArrayList<>();
    private boolean isRunning = false;
    private final String pipelineConfigPrefix;

    protected AbstractPipeline(Class<RECEIVER> receiverClass,
                               Class<EXPORTER> exporterClass,
                               PipelineConfig pipelineConfig,
                               ObjectMapper objectMapper) {
        // Get the prefix of the configuration properties for further dynamic property change handling
        Class<?> configClass = pipelineConfig.getClass();
        if (configClass.getName().contains("SpringCGLIB$$")) {
            configClass = configClass.getSuperclass();
        }
        ConfigurationProperties properties = configClass.getAnnotation(ConfigurationProperties.class);
        Preconditions.checkNotNull(properties, "PipelineConfig class must be annotated with @ConfigurationProperties");
        this.pipelineConfigPrefix = properties.prefix();

        this.pipelineConfig = pipelineConfig;
        this.receivers = createReceivers(pipelineConfig, objectMapper, receiverClass);
        this.processors = createProcessors(pipelineConfig, objectMapper);

        this.objectMapper = objectMapper;

        initializeExportersFromConfig(pipelineConfig, objectMapper, exporterClass);
    }

    public ITransformer[] getCopyOfProcessors() {
        // Create a new copy of processors to avoid concurrent modification exception
        return processors.toArray(new ITransformer[0]);
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

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        if (event.getKeys()
                 .stream()
                 .noneMatch((key) -> key.startsWith(this.pipelineConfigPrefix))) {
            return;
        }

        PipelineConfig pipelineConfig = EnvironmentBinder.from((ConfigurableEnvironment) this.applicationContext.getEnvironment())
                                                         .bind(this.pipelineConfigPrefix, PipelineConfig.class);
        if (pipelineConfig == null) {
            return;
        }
        try {
            getLogger().info("Reloading processors:\n{}", this.objectMapper.copy()
                                                                           .configure(SerializationFeature.INDENT_OUTPUT, true)
                                                                           .writeValueAsString(pipelineConfig.getProcessors()));
        } catch (JsonProcessingException ignored) {
        }
        this.processors = this.createProcessors(pipelineConfig, this.objectMapper);
    }
}
