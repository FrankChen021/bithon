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

import org.bithon.agent.configuration.annotation.ConfigurationProperties;
import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.shaded.com.fasterxml.jackson.databind.SerializationFeature;

import javax.annotation.processing.AbstractProcessor;
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
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
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
@SupportedAnnotationTypes("org.bithon.agent.configuration.annotation.ConfigurationProperties")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class MetadataProcessor extends AbstractProcessor {

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

        // Process all fields in the class hierarchy (including inherited fields)
        processClassHierarchyFields(configClass, basePath, isDynamic, allProperties);

        // Process nested configuration classes
        processNestedConfigurationClasses(configClass, basePath, isDynamic, allProperties);
    }

    /**
     * Process fields from the current class and all its parent classes in the hierarchy.
     * This ensures that inherited fields are also included in the configuration metadata.
     *
     * @param currentClass  the class to process
     * @param basePath      the base configuration path
     * @param isDynamic     whether the configuration is dynamic
     * @param allProperties the list to add discovered properties to
     */
    private void processClassHierarchyFields(TypeElement currentClass, String basePath, boolean isDynamic, List<PropertyMetadata> allProperties) {
        // Process fields in the current class and all parent classes, but always use the original annotated class
        // as the configuration class for all properties
        processClassHierarchyFieldsRecursive(currentClass, currentClass, basePath, isDynamic, allProperties);
    }

    /**
     * Recursively process fields from the class hierarchy.
     *
     * @param originalClass the original @ConfigurationProperties annotated class
     * @param currentClass  the current class being processed in the hierarchy
     * @param basePath      the base configuration path
     * @param isDynamic     whether the configuration is dynamic
     * @param allProperties the list to add discovered properties to
     */
    private void processClassHierarchyFieldsRecursive(TypeElement originalClass, TypeElement currentClass, String basePath, boolean isDynamic, List<PropertyMetadata> allProperties) {
        // Process fields in the current class
        processClassFields(originalClass, currentClass, basePath, isDynamic, allProperties);

        // Recursively process parent class fields
        TypeMirror superclass = currentClass.getSuperclass();
        if (superclass != null) {
            Element superElement = processingEnv.getTypeUtils().asElement(superclass);
            if (superElement != null && superElement.getKind() == ElementKind.CLASS) {
                TypeElement superTypeElement = (TypeElement) superElement;

                // Only process parent classes that are not system classes (like Object, etc.)
                String superClassName = superTypeElement.getQualifiedName().toString();
                if (!isSystemClass(superClassName)) {
                    processClassHierarchyFieldsRecursive(originalClass, superTypeElement, basePath, isDynamic, allProperties);
                }
            }
        }
    }

    /**
     * Process fields directly declared in the given class (not inherited).
     *
     * @param originalClass the original @ConfigurationProperties annotated class
     * @param currentClass  the current class being processed (maybe a parent class)
     * @param basePath      the base configuration path
     * @param isDynamic     whether the configuration is dynamic
     * @param allProperties the list to add discovered properties to
     */
    private void processClassFields(TypeElement originalClass,
                                    TypeElement currentClass,
                                    String basePath,
                                    boolean isDynamic,
                                    List<PropertyMetadata> allProperties) {
        for (Element enclosedElement : currentClass.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;

                // Use centralized field filtering logic
                if (shouldSkipField(currentClass, field)) {
                    continue;
                }

                // Always use the original annotated class as the configuration class
                PropertyMetadata property = createPropertyMetadata(field, basePath, isDynamic, getBinaryName(originalClass));
                allProperties.add(property);
            }
        }
    }

    private PropertyMetadata createPropertyMetadata(VariableElement field,
                                                    String basePath,
                                                    boolean isDynamic,
                                                    String configurationClass) {
        String fieldName = field.getSimpleName().toString();
        String fullPath = basePath + "." + fieldName;

        PropertyMetadata property = new PropertyMetadata();
        property.path = fullPath;
        property.dynamic = isDynamic;
        property.configurationClass = configurationClass;
        property.type = normalizedTypeName(field.asType().toString());
        property.required = isFieldRequired(field);

        // Extract description from @PropertyDescriptor if present
        Map<String, String> descriptorAnnotation = extractPropertyDescriptor(field);
        property.description = descriptorAnnotation.get("description");
        property.suggestion = descriptorAnnotation.get("suggestion");

        // Extract default value from field initialization
        Object constantValue = field.getConstantValue();
        if (constantValue != null) {
            property.defaultValue = constantValue.toString();
        }

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

    private void processNestedFields(TypeElement typeElement, String propertyPath, boolean isDynamic, List<PropertyMetadata> allProperties) {
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.FIELD) {
                VariableElement field = (VariableElement) enclosedElement;

                // Use centralized field filtering logic
                if (shouldSkipField(typeElement, field)) {
                    continue;
                }

                PropertyMetadata property = createPropertyMetadata(field, propertyPath, isDynamic, getBinaryName(typeElement));
                allProperties.add(property);

                // Recursively process further nested classes
                TypeMirror fieldType = field.asType();
                Element fieldTypeElement = processingEnv.getTypeUtils().asElement(fieldType);

                if (fieldTypeElement != null && fieldTypeElement.getKind() == ElementKind.CLASS) {
                    TypeElement fieldTypeClass = (TypeElement) fieldTypeElement;
                    if (hasConfigurationFields(fieldTypeClass)) {
                        String furtherNestedPath = propertyPath + "." + field.getSimpleName().toString();
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

    private String normalizedTypeName(String fullTypeName) {
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

    /**
     * Determines if a field should be skipped from configuration metadata generation.
     * Fields are skipped if they are:
     * - Static fields
     * - Annotated with @Deprecated
     * - Annotated with @JsonIgnore (from Jackson)
     * - Don't have a public getter method
     * - Are user-defined classes (not primitive types or standard library types)
     *
     * @param field       the field to check
     * @param typeElement the class containing the field (used for getter validation)
     * @return true if the field should be skipped, false otherwise
     */
    private boolean shouldSkipField(TypeElement typeElement, VariableElement field) {
        // Skip static fields and synthetic fields
        if (field.getModifiers().contains(Modifier.STATIC)) {
            return true;
        }

        // Check for annotation-based exclusions
        for (AnnotationMirror annotationMirror : field.getAnnotationMirrors()) {
            String annotationName = annotationMirror.getAnnotationType().toString();

            // Check for @Deprecated annotation
            if ("java.lang.Deprecated".equals(annotationName)) {
                return true;
            }

            // Check for @JsonIgnore annotation (both shaded and non-shaded versions)
            if ("org.bithon.shaded.com.fasterxml.jackson.annotation.JsonIgnore".equals(annotationName) ||
                "com.fasterxml.jackson.annotation.JsonIgnore".equals(annotationName)) {
                return true;
            }
        }

        // Skip fields that don't have public getter methods
        if (!hasPublicGetter(typeElement, field)) {
            return true;
        }

        // Skip fields that are user-defined classes (not primitive or standard library types)
        if (isUserDefinedClass(field)) {
            return true;
        }

        return false;
    }

    /**
     * Determines if a field is of a user-defined class type that should be excluded from metadata.
     * Returns true for simple user-defined classes, false for primitive types, standard library types,
     * and user-defined classes that contain configurable properties.
     *
     * @param field the field to check
     * @return true if the field is a user-defined class that should be excluded
     */
    private boolean isUserDefinedClass(VariableElement field) {
        TypeMirror fieldType = field.asType();
        if (fieldType instanceof ArrayType) {
            fieldType = ((ArrayType) fieldType).getComponentType();
        }
        String fieldTypeName = fieldType.toString();

        // Allow primitive types
        if (isPrimitiveType(fieldTypeName)) {
            return false;
        }

        // Allow standard library and utility types (including collections)
        if (isAllowedType(fieldTypeName)) {
            return false;
        }

        processingEnv.getMessager()
                     .printMessage(Diagnostic.Kind.WARNING,
                                   StringUtils.format("field %s has type %s which is not supported in configuration change.", field.getSimpleName(), field.asType().toString()));

        return true;
    }

    /**
     * Checks if a type is a primitive type or primitive wrapper.
     */
    private boolean isPrimitiveType(String typeName) {
        return "boolean".equals(typeName) ||
               "byte".equals(typeName) ||
               "char".equals(typeName) ||
               "short".equals(typeName) ||
               "int".equals(typeName) ||
               "long".equals(typeName) ||
               "float".equals(typeName) ||
               "double".equals(typeName) ||
               "java.lang.Boolean".equals(typeName) ||
               "java.lang.Byte".equals(typeName) ||
               "java.lang.Character".equals(typeName) ||
               "java.lang.Short".equals(typeName) ||
               "java.lang.Integer".equals(typeName) ||
               "java.lang.Long".equals(typeName) ||
               "java.lang.Float".equals(typeName) ||
               "java.lang.Double".equals(typeName);
    }

    /**
     * Checks if a type is an allowed standard library or utility type.
     */
    private boolean isAllowedType(String typeName) {
        // Handle generic types by extracting the base type
        String baseType = typeName;
        if (typeName.contains("<")) {
            baseType = typeName.substring(0, typeName.indexOf("<"));
        }

        // Allow common Java standard library types
        if ("java.lang.String".equals(baseType) ||
            "java.lang.Object".equals(baseType) ||
            baseType.startsWith("java.util.") ||
            baseType.startsWith("java.time.") ||
            baseType.startsWith("java.math.")) {
            return true;
        }

        // Allow Bithon utility classes that should be treated as primitive-like
        if ("org.bithon.component.commons.utils.HumanReadableNumber".equals(baseType) ||
            "org.bithon.component.commons.utils.HumanReadableDuration".equals(baseType) ||
            "org.bithon.component.commons.utils.HumanReadablePercentage".equals(baseType)) {
            return true;
        }

        return false;
    }

    /**
     * Checks if a field has a public getter method in the class hierarchy.
     * For boolean fields, checks for both getFieldName() and isFieldName() methods.
     *
     * @param typeElement the class containing the field
     * @param field       the field to check
     * @return true if a public getter exists, false otherwise
     */
    private boolean hasPublicGetter(TypeElement typeElement, VariableElement field) {
        String fieldName = field.getSimpleName().toString();
        String capitalizedFieldName = Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);

        // Check for getter methods in the class hierarchy
        return findGetterRecursively(typeElement, field, capitalizedFieldName);
    }

    /**
     * Recursively checks for getter methods in the class hierarchy.
     *
     * @param typeElement          the current class to check
     * @param field                the field we're looking for a getter for
     * @param capitalizedFieldName the capitalized field name for getter method names
     * @return true if a getter is found, false otherwise
     */
    private boolean findGetterRecursively(TypeElement typeElement, VariableElement field, String capitalizedFieldName) {
        // Check methods in the current class
        for (Element enclosedElement : typeElement.getEnclosedElements()) {
            if (enclosedElement.getKind() == ElementKind.METHOD) {
                ExecutableElement method = (ExecutableElement) enclosedElement;

                // Check for standard getter patterns
                if (isGetterMethod(method, capitalizedFieldName, field)) {
                    return true;
                }
            }
        }

        // Recursively check parent classes
        TypeMirror superclass = typeElement.getSuperclass();
        if (superclass != null) {
            Element superElement = processingEnv.getTypeUtils().asElement(superclass);
            if (superElement != null && superElement.getKind() == ElementKind.CLASS) {
                TypeElement superTypeElement = (TypeElement) superElement;

                // Only check parent classes that are not system classes
                String superClassName = superTypeElement.getQualifiedName().toString();
                if (!isSystemClass(superClassName)) {
                    return findGetterRecursively(superTypeElement, field, capitalizedFieldName);
                }
            }
        }

        return false;
    }

    /**
     * Checks if a method name matches the getter pattern for a field.
     *
     * @param method               the method to check
     * @param capitalizedFieldName the capitalized field name
     * @param field                the field element (used to check if it's boolean)
     * @return true if the method is a valid getter for the field
     */
    private boolean isGetterMethod(ExecutableElement method, String capitalizedFieldName, VariableElement field) {
        // Skip non-public methods
        if (!method.getModifiers().contains(Modifier.PUBLIC)) {
            return false;
        }

        // Skip methods with parameters (getters should have no parameters)
        if (!method.getParameters().isEmpty()) {
            return false;
        }

        String methodName = method.getSimpleName().toString();

        // Standard getter: getFieldName()
        if (("get" + capitalizedFieldName).equals(methodName)) {
            return true;
        }

        // Boolean getter: isFieldName() - only for boolean fields
        if (("is" + capitalizedFieldName).equals(methodName)) {
            String fieldType = field.asType().toString();
            return "boolean".equals(fieldType) || "java.lang.Boolean".equals(fieldType);
        }

        return false;
    }

    private Map<String, String> extractPropertyDescriptor(VariableElement field) {
        Map<String, String> properties = new HashMap<>();

        // Look for @PropertyDescriptor annotation using annotation mirrors
        for (AnnotationMirror annotationMirror : field.getAnnotationMirrors()) {
            String annotationName = annotationMirror.getAnnotationType().toString();
            if ("org.bithon.agent.configuration.annotation.PropertyDescriptor".equals(annotationName)) {
                // Extract the description value from the annotation
                Map<? extends ExecutableElement, ? extends AnnotationValue> elementValues = annotationMirror.getElementValues();

                for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry : elementValues.entrySet()) {
                    String name = entry.getKey().getSimpleName().toString();
                    String value = entry.getValue().getValue().toString();

                    properties.put(name, value);
                }
            }
        }

        return properties;
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
        String configClass = properties.get(0).configurationClass;

        // For plugin classes like "org.bithon.agent.plugin.bithon.brpc.BithonBrpcPlugin"
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
                // For a single part like "redis.RedisPlugin" -> "redis"
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

    /**
     * Get the binary name for a TypeElement, which correctly handles inner classes
     * by using '$' instead of '.' as the separator.
     *
     * @param typeElement the type element
     * @return the binary name suitable for runtime class loading
     */
    private String getBinaryName(TypeElement typeElement) {
        return processingEnv.getElementUtils().getBinaryName(typeElement).toString();
    }
}

