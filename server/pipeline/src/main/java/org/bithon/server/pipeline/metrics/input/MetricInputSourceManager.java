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

package org.bithon.server.pipeline.metrics.input;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.AnnotatedClassResolver;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.TextNode;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.storage.datasource.ISchema;
import org.bithon.server.storage.datasource.SchemaManager;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/16 21:35
 */
@Slf4j
public class MetricInputSourceManager implements SchemaManager.ISchemaChangedListener {

    private final Map<String, IMetricInputSource> inputSources = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper;
    private final SchemaManager schemaManager;
    private final boolean enabled;
    private final Map<String, Class<? extends IMetricInputSource>> subTypes = new HashMap<>();
    private boolean suppressListener;

    public MetricInputSourceManager(boolean enabled,
                                    SchemaManager schemaManager,
                                    ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.schemaManager = schemaManager;
        this.enabled = enabled;
        if (this.enabled) {
            this.schemaManager.addListener(this);
        }
        this.suppressListener = false;
    }

    public void start(Class<? extends IMetricInputSource> inputSourceClazz) {
        if (!enabled) {
            log.warn("Metric log storage is NOT configured to be enabled by property 'bithon.storage.metric.enabled', the input source [{}] does not take effect.", inputSourceClazz.getSimpleName());
            return;
        }

        // Find the registered 'type' property
        AnnotatedClass superType = AnnotatedClassResolver.resolveWithoutSuperTypes(this.objectMapper.getSerializationConfig(), IMetricInputSource.class);
        Collection<NamedType> registeredSubtypes = this.objectMapper.getSubtypeResolver()
                                                                    .collectAndResolveSubtypesByClass(objectMapper.getSerializationConfig(),
                                                                                                      superType);
        NamedType type = registeredSubtypes.stream()
                                           .filter((namedType) -> namedType.getType().equals(inputSourceClazz))
                                           .findFirst()
                                           .orElseThrow(() -> new RuntimeException(StringUtils.format("class [%s] does not register as sub type of [%s]", inputSourceClazz.getName(), IMetricInputSource.class.getName())));

        // register the type
        subTypes.putIfAbsent(type.getName(), inputSourceClazz);

        this.suppressListener = true;
        try {
            schemaManager.getSchemas()
                         .values()
                         .forEach((schema) -> startInputSource(schema, type.getName()));
        } finally {
            this.suppressListener = false;
        }
    }

    @Override
    public void onSchemaChanged(ISchema oldSchema, ISchema newSchema) {
        if (suppressListener) {
            return;
        }

        if (oldSchema != null
            && Objects.equals(oldSchema.getSignature(), newSchema.getSignature())) {
            // same signature, do nothing
            return;
        }

        // stop input
        if (oldSchema != null && oldSchema.getInputSourceSpec() != null) {
            log.info("Stop input source for schema [{}]", oldSchema.getName());
            IMetricInputSource inputSource = inputSources.remove(oldSchema.getSignature());
            if (inputSource != null) {
                inputSource.stop();
            }
        }

        // start for the new schema
        startInputSource(newSchema);
    }

    private void startInputSource(ISchema schema) {
        startInputSource(schema, null);
    }

    private void startInputSource(ISchema schema, String subType) {
        if (schema.isVirtual()) {
            return;
        }

        JsonNode inputSourceSpec = schema.getInputSourceSpec();
        if (inputSourceSpec == null) {
            return;
        }
        if (NullNode.getInstance().equals(inputSourceSpec)) {
            return;
        }
        JsonNode typeNode = inputSourceSpec.get("type");
        if (typeNode == null || NullNode.getInstance().equals(typeNode)) {
            return;
        }
        if (!(typeNode instanceof TextNode)) {
            return;
        }

        String type = typeNode.asText();
        if (subType != null && !subType.equals(type)) {
            return;
        }

        Class<? extends IMetricInputSource> inputSourceType = this.subTypes.get(type);
        if (inputSourceType == null) {
            return;
        }

        log.info("Start input source for schema [{}]", schema.getName());
        IMetricInputSource inputSource = objectMapper.convertValue(schema.getInputSourceSpec(), inputSourceType);
        inputSource.start(schema);
        inputSources.put(schema.getSignature(), inputSource);
    }
}
