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

package org.bithon.agent.configuration.processor;

import com.google.auto.service.AutoService;
import org.bithon.agent.configuration.annotation.ConfigurationProperties;
import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor that generates configuration metadata at compile time.
 * Processes classes annotated with {@link ConfigurationProperties} and extracts
 * metadata for all their properties.
 *
 * @author frank.chen021@outlook.com
 */
@AutoService(Processor.class)
@SupportedAnnotationTypes("org.bithon.agent.configuration.annotation.ConfigurationProperties")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class ConfigurationMetadataProcessor extends AbstractProcessor {

    private static final String METADATA_FILE_BASE_PATH = "META-INF/bithon/configuration/";

    /**
     * Process all classes annotated with @ConfigurationProperties
     */
    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }

        List<PropertyMetadata> allProperties = new ArrayList<>();
        for (Element element : roundEnv.getElementsAnnotatedWith(ConfigurationProperties.class)) {
            if (element.getKind() == ElementKind.CLASS) {
                TypeElement typeElement = (TypeElement) element;
                processConfigurationClass(typeElement, allProperties);
            }
        }

        // Generate metadata file if we have properties
        if (!allProperties.isEmpty()) {
            try {
                generateMetadataFile(allProperties);
            } catch (IOException e) {
                processingEnv.getMessager()
                             .printMessage(Diagnostic.Kind.ERROR,
                                           "Failed to generate configuration metadata: " + e);
            }
        }

        return true;
    }

    private void processConfigurationClass(TypeElement configClass, List<PropertyMetadata> allProperties) {
        ConfigurationProperties configProps = configClass.getAnnotation(ConfigurationProperties.class);
        if (configProps == null) {
            return;
        }

        String basePath = configProps.path();
        boolean isDynamic = configProps.dynamic();

        processingEnv.getMessager()
                     .printMessage(Diagnostic.Kind.NOTE, "Processing ConfigurationProperties annotated class: " + configClass.getQualifiedName());

        // Process all fields in the class
        for (Element enclosedElement : configClass.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;

                // Skip static fields and synthetic fields
                if (field.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                PropertyMetadata property = createPropertyMetadata(field, basePath, isDynamic, configClass.getQualifiedName().toString());
                allProperties.add(property);
            }
        }

        // Process nested configuration classes
        processNestedConfigurationClasses(configClass, basePath, isDynamic, allProperties);
    }

    private PropertyMetadata createPropertyMetadata(VariableElement field,
                                                    String basePath,
                                                    boolean isDynamic,
                                                    String configurationClass) {
        String fieldName = field.getSimpleName().toString();
        String fullPath = basePath + "." + fieldName;

        PropertyMetadata property = new PropertyMetadata();
        property.setPath(fullPath);
        property.setDynamic(isDynamic);
        property.setConfigurationClass(configurationClass);

        // Set type information
        TypeMirror fieldType = field.asType();
        property.setType(getSimpleTypeName(fieldType.toString()));

        // Extract description from @PropertyDescriptor if present
        String description = extractPropertyDescription(field);
        if (description != null && !description.isEmpty()) {
            property.setDescription(description);
        } else {
            // Generate description from field name
            property.setDescription(generateDescriptionFromFieldName(fieldName));
        }

        // Extract default value from field initialization
        Object constantValue = field.getConstantValue();
        if (constantValue != null) {
            property.setDefaultValue(constantValue.toString());
        }

        // Check if field is required based on validation annotations
        property.setRequired(isFieldRequired(field));

        return property;
    }

    private void processNestedConfigurationClasses(TypeElement parentClass, String parentPath, boolean isDynamic, List<PropertyMetadata> allProperties) {
        // Process fields that are themselves configuration classes
        for (Element enclosedElement : parentClass.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;

                if (field.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                TypeMirror fieldType = field.asType();
                Element fieldTypeElement = processingEnv.getTypeUtils().asElement(fieldType);

                if (fieldTypeElement != null && fieldTypeElement.getKind() == ElementKind.CLASS) {
                    TypeElement fieldTypeClass = (TypeElement) fieldTypeElement;

                    // Check if this field type has configuration properties
                    if (hasConfigurationFields(fieldTypeClass)) {
                        String nestedPath = parentPath + "." + field.getSimpleName().toString();
                        processNestedFields(fieldTypeClass, nestedPath, isDynamic, allProperties);
                    }
                }
            }
        }
    }

    private void processNestedFields(TypeElement nestedClass, String nestedPath, boolean isDynamic, List<PropertyMetadata> allProperties) {
        for (Element enclosedElement : nestedClass.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;

                if (field.getModifiers().contains(Modifier.STATIC)) {
                    continue;
                }

                PropertyMetadata property = createPropertyMetadata(field, nestedPath, isDynamic, nestedClass.getQualifiedName().toString());
                allProperties.add(property);

                // Recursively process further nested classes
                TypeMirror fieldType = field.asType();
                Element fieldTypeElement = processingEnv.getTypeUtils().asElement(fieldType);

                if (fieldTypeElement != null && fieldTypeElement.getKind() == ElementKind.CLASS) {
                    TypeElement fieldTypeClass = (TypeElement) fieldTypeElement;
                    if (hasConfigurationFields(fieldTypeClass)) {
                        String furtherNestedPath = nestedPath + "." + field.getSimpleName().toString();
                        processNestedFields(fieldTypeClass, furtherNestedPath, isDynamic, allProperties);
                    }
                }
            }
        }
    }

    private boolean hasConfigurationFields(TypeElement typeElement) {
        String className = typeElement.getQualifiedName().toString();

        // Skip JDK internal classes and common library classes
        if (isSystemClass(className)) {
            return false;
        }

        // Check if the class has any non-static fields that could be configuration properties
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;
                if (!field.getModifiers().contains(Modifier.STATIC)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isSystemClass(String className) {
        // Skip JDK classes
        if (className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("sun.") ||
            className.startsWith("com.sun.") ||
            className.startsWith("jdk.")) {
            return true;
        }

        // Skip common library classes that shouldn't be processed as configuration
        if (className.startsWith("org.slf4j.") ||
            className.startsWith("ch.qos.logback.") ||
            className.startsWith("org.apache.commons.") ||
            className.startsWith("com.fasterxml.jackson.") ||
            className.startsWith("org.springframework.") ||
            className.startsWith("io.netty.")) {
            return true;
        }

        // Skip utility classes that should be treated as primitive types
        if ("org.bithon.component.commons.utils.HumanReadableNumber".equals(className) ||
            "org.bithon.component.commons.utils.HumanReadableDuration".equals(className) ||
            "org.bithon.component.commons.utils.HumanReadablePercentage".equals(className)) {
            return true;
        }

        // Only process classes from the current project (org.bithon)
        return !className.startsWith("org.bithon.");
    }

    private String getSimpleTypeName(String fullTypeName) {
        // Handle generic types
        if (fullTypeName.contains("<")) {
            return fullTypeName; // Keep generic information
        }

        // Extract simple name for non-generic types
        int lastDot = fullTypeName.lastIndexOf('.');
        if (lastDot >= 0) {
            return fullTypeName.substring(lastDot + 1);
        }
        return fullTypeName;
    }

    private String generateDescriptionFromFieldName(String fieldName) {
        // Convert camelCase to readable description
        StringBuilder description = new StringBuilder();
        for (int i = 0; i < fieldName.length(); i++) {
            char c = fieldName.charAt(i);
            if (i > 0 && Character.isUpperCase(c)) {
                description.append(' ');
            }
            if (i == 0) {
                description.append(Character.toUpperCase(c));
            } else {
                description.append(Character.toLowerCase(c));
            }
        }
        return description.toString();
    }

    private boolean isFieldRequired(VariableElement field) {
        // Check for validation annotations that indicate required fields
        return field.getAnnotationMirrors()
                    .stream()
                    .anyMatch(annotation -> {
                        String annotationName = annotation.getAnnotationType().toString();
                        return annotationName.contains("NotNull") ||
                               annotationName.contains("NotBlank") ||
                               annotationName.contains("NotEmpty");
                    });
    }

    private String extractPropertyDescription(VariableElement field) {
        // Look for @PropertyDescriptor annotation using annotation mirrors
        for (AnnotationMirror annotationMirror : field.getAnnotationMirrors()) {
            String annotationName = annotationMirror.getAnnotationType().toString();
            if ("org.bithon.agent.configuration.annotation.PropertyDescriptor".equals(annotationName)) {
                // Extract the description value from the annotation
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    if ("description".equals(entry.getKey().getSimpleName().toString())) {
                        Object value = entry.getValue().getValue();
                        if (value instanceof String) {
                            return (String) value;
                        }
                    }
                }
            }
        }
        return null;
    }

    private void generateMetadataFile(List<PropertyMetadata> properties) throws IOException {
        // Determine module name from the first property's configuration class
        String moduleName = determineModuleName(properties);
        String metadataFilePath = METADATA_FILE_BASE_PATH + moduleName + ".meta";

        FileObject file = processingEnv.getFiler()
                                       .createResource(StandardLocation.CLASS_OUTPUT, "", metadataFilePath);

        try (Writer writer = file.openWriter()) {
            new ObjectMapper()
                .enable(SerializationFeature.INDENT_OUTPUT)
                .writeValue(writer, properties);
        }

        processingEnv.getMessager()
                     .printMessage(Diagnostic.Kind.NOTE,
                                   "Generated configuration metadata: " + metadataFilePath);
    }

    private String determineModuleName(List<PropertyMetadata> properties) {
        if (properties.isEmpty()) {
            return "unknown";
        }

        // Extract module name from the configuration class package
        String configClass = properties.get(0).getConfigurationClass();

        // For plugin classes like "org.bithon.agent.plugin.bithon.brpc.BithonBrpcPlugin.ServiceProviderConfig"
        // or "org.bithon.agent.plugin.spring.bean.installer.SpringBeanPluginConfig"
        // Extract the module identifier
        if (configClass.startsWith("org.bithon.agent.plugin.")) {
            String pluginPart = configClass.substring("org.bithon.agent.plugin.".length());

            // Handle different plugin package structures
            String[] parts = pluginPart.split("\\.");
            if (parts.length >= 2) {
                // For "bithon.brpc.BithonBrpcPlugin" -> "bithon-brpc"
                // For "spring.bean.installer.SpringBeanPluginConfig" -> "spring-bean"
                return parts[0] + "-" + parts[1];
            } else if (parts.length == 1) {
                // For single part like "redis.RedisPlugin" -> "redis"
                return parts[0];
            }
        }

        // For non-plugin classes like "org.bithon.agent.controller.config.ConfigurationCommandImpl"
        // Extract a meaningful module name from the package
        if (configClass.contains("org.bithon.agent.")) {
            String agentPart = configClass.substring("org.bithon.agent.".length());
            String[] parts = agentPart.split("\\.");
            if (parts.length >= 1) {
                return "agent-" + parts[0];
            }
        }

        // Fallback: use a hash of the configuration class name
        return "module-" + Math.abs(configClass.hashCode());
    }
}
