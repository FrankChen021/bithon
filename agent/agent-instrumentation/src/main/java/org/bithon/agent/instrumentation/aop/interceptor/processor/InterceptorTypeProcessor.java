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

package org.bithon.agent.instrumentation.aop.interceptor.processor;

import org.bithon.agent.instrumentation.aop.interceptor.InterceptorType;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AfterInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.AroundInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.BeforeInterceptor;
import org.bithon.agent.instrumentation.aop.interceptor.declaration.ReplaceInterceptor;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Annotation processor that analyzes interceptor classes at compile time
 * to determine their type (BEFORE, AFTER, AROUND, REPLACEMENT) based on inheritance hierarchy.
 * Generates a registry class with static mappings to at the package where plugin class is located.
 * This generated class will be loaded during a plugin is loaded, and the interceptor types
 *
 * @author frank.chen021@outlook.com
 * @date 2024/1/1 00:00
 */
@SupportedAnnotationTypes("*")
public class InterceptorTypeProcessor extends AbstractProcessor {

    private final Map<String, InterceptorType> interceptorTypes = new HashMap<>();
    private String pluginClass = null;
    private boolean processed = false;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        // Always print this message to see if processor runs
        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "#### InterceptorTypeProcessor: Running! Annotations: " + annotations.size() + ", processingOver: " + roundEnv.processingOver());

        if (roundEnv.processingOver()) {
            // Generate the registry class on the final round
            if (!processed) {
                generateRegistryClass();
                processed = true;
            }
            return true;
        }

        // Get the type mirrors for our base interceptor classes
        TypeMirror beforeType = getTypeMirror(BeforeInterceptor.class.getName());
        TypeMirror afterType = getTypeMirror(AfterInterceptor.class.getName());
        TypeMirror aroundType = getTypeMirror(AroundInterceptor.class.getName());
        TypeMirror replaceType = getTypeMirror(ReplaceInterceptor.class.getName());

        if (beforeType == null || afterType == null || aroundType == null || replaceType == null) {
            // Base interceptor types not available in this compilation unit
            return false;
        }

        Types typeUtils = processingEnv.getTypeUtils();

        // Process all class elements in this round
        for (Element rootElement : roundEnv.getRootElements()) {
            if (rootElement.getKind() == ElementKind.CLASS) {
                TypeElement classElement = (TypeElement) rootElement;

                String className = classElement.getQualifiedName().toString();

                // Only process classes under org.bithon.agent.plugin package
                if (!className.startsWith("org.bithon.agent.plugin.")) {
                    continue;
                }

                // Skip the base interceptor classes themselves
                if (isBaseInterceptorClass(className)) {
                    continue;
                }

                // Debug: Print all classes being processed (NOTE level - may not show by default)
                processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "InterceptorTypeProcessor: Processing class " + className);

                // Check if this is a plugin class (ends with "Plugin" and implements IPlugin)
                if (className.endsWith("Plugin") && implementsIPlugin(classElement, typeUtils)) {
                    if (pluginClass == null) {
                        pluginClass = className;
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Found plugin class: " + className);
                    } else {
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                                 "Warning: Multiple plugin classes found in same compilation unit: " + pluginClass + " and " + className);
                    }
                }

                // Check if this class extends any of our base interceptor types
                InterceptorType type = determineInterceptorType(classElement, typeUtils,
                                                                beforeType, afterType, aroundType, replaceType);

                if (type != null) {
                    interceptorTypes.put(className, type);
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING, "Found interceptor: " + className + " -> " + type);
                }
            }
        }

        return false;
    }

    private TypeMirror getTypeMirror(String className) {
        TypeElement element = processingEnv.getElementUtils().getTypeElement(className);
        return element != null ? element.asType() : null;
    }

    private boolean isBaseInterceptorClass(String className) {
        return BeforeInterceptor.class.getName().equals(className) ||
               AfterInterceptor.class.getName().equals(className) ||
               AroundInterceptor.class.getName().equals(className) ||
               ReplaceInterceptor.class.getName().equals(className);
    }

    private InterceptorType determineInterceptorType(TypeElement classElement, Types typeUtils,
                                                     TypeMirror beforeType, TypeMirror afterType,
                                                     TypeMirror aroundType, TypeMirror replaceType) {

        TypeMirror currentType = classElement.asType();

        // Check direct inheritance and walk up the inheritance hierarchy
        while (currentType != null) {
            if (typeUtils.isAssignable(currentType, beforeType) && !typeUtils.isSameType(currentType, beforeType)) {
                return InterceptorType.BEFORE;
            }
            if (typeUtils.isAssignable(currentType, afterType) && !typeUtils.isSameType(currentType, afterType)) {
                return InterceptorType.AFTER;
            }
            if (typeUtils.isAssignable(currentType, aroundType) && !typeUtils.isSameType(currentType, aroundType)) {
                return InterceptorType.AROUND;
            }
            if (typeUtils.isAssignable(currentType, replaceType) && !typeUtils.isSameType(currentType, replaceType)) {
                return InterceptorType.REPLACEMENT;
            }

            // Move to superclass
            TypeElement typeElement = (TypeElement) typeUtils.asElement(currentType);
            if (typeElement == null) {
                break;
            }

            currentType = typeElement.getSuperclass();
            if (currentType.getKind().name().equals("NONE")) {
                break;
            }
        }

        return null;
    }

    private void generateRegistryClass() {
        if (interceptorTypes.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE,
                                                     "No interceptors found, skipping registry generation");
            return;
        }

        // Determine module name - this will report errors if configuration is invalid
        String moduleName = determineModuleName();
        if (moduleName == null) {
            // Error already reported in determineModuleName()
            return;
        }

        // Generate the mapping class
        generateInterceptorTypesClass();
    }

    private void generateInterceptorTypesClass() {
        try {
            // Generate mapping class in the same package as the plugin
            String moduleName = determineModuleName();
            if (moduleName == null) {
                // Error already reported in determineModuleName()
                return;
            }
            String packageName = "org.bithon.agent.plugin." + moduleName;
            String className = "InterceptorTypes";
            String fullClassName = packageName + "." + className;

            // Create the mapping class file
            FileObject file = processingEnv.getFiler().createSourceFile(fullClassName);

            try (Writer writer = file.openWriter()) {
                writer.write("/*\n");
                writer.write(" *    Copyright 2020 bithon.org\n");
                writer.write(" *\n");
                writer.write(" *    Licensed under the Apache License, Version 2.0 (the \"License\");\n");
                writer.write(" *    you may not use this file except in compliance with the License.\n");
                writer.write(" *    You may obtain a copy of the License at\n");
                writer.write(" *\n");
                writer.write(" *        http://www.apache.org/licenses/LICENSE-2.0\n");
                writer.write(" *\n");
                writer.write(" *    Unless required by applicable law or agreed to in writing, software\n");
                writer.write(" *    distributed under the License is distributed on an \"AS IS\" BASIS,\n");
                writer.write(" *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.\n");
                writer.write(" *    See the License for the specific language governing permissions and\n");
                writer.write(" *    limitations under the License.\n");
                writer.write(" */\n");
                writer.write("\n");
                writer.write("package " + packageName + ";\n");
                writer.write("\n");
                writer.write("import org.bithon.agent.instrumentation.aop.interceptor.InterceptorType;\n");
                writer.write("import java.util.HashMap;\n");
                writer.write("import java.util.Map;\n");
                writer.write("\n");
                writer.write("/**\n");
                writer.write(" * Generated interceptor type mappings for this plugin module.\n");
                writer.write(" * This class is automatically generated by InterceptorTypeProcessor.\n");
                writer.write(" *\n");
                writer.write(" * @author Generated by InterceptorTypeProcessor\n");
                writer.write(" */\n");
                writer.write("public final class " + className + " {\n");
                writer.write("\n");
                writer.write("    /**\n");
                writer.write("     * Returns the interceptor types for this plugin.\n");
                writer.write("     * @return Map of interceptor class name to InterceptorType\n");
                writer.write("     */\n");
                writer.write("    public static Map<String, InterceptorType> getTypes() {\n");
                writer.write("        Map<String, InterceptorType> types = new HashMap<>();\n");

                // Generate the types
                for (Map.Entry<String, InterceptorType> entry : interceptorTypes.entrySet()) {
                    writer.write("        types.put(\"" + entry.getKey() + "\", InterceptorType." + entry.getValue() + ");\n");
                }

                writer.write("        return types;\n");
                writer.write("    }\n");
                writer.write("}\n");
            }

            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                     "Generated " + fullClassName + " with " + interceptorTypes.size() + " interceptors");

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     "Failed to generate interceptor type mapping: " + e.getMessage());
        }
    }

    private String determineModuleName() {
        // Check if we have interceptors but no plugin class - this is an error
        if (pluginClass == null && !interceptorTypes.isEmpty()) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                     "Found " + interceptorTypes.size() + " interceptor classes but no plugin class implementing IPlugin. " +
                                                     "Each plugin module must have exactly one class ending with 'Plugin' that implements IPlugin. " +
                                                     "Interceptors found: " + interceptorTypes.keySet());
            return null;
        }

        // If no plugin class and no interceptors, nothing to process
        if (pluginClass == null) {
            return null;
        }

        // Extract module name from the plugin class we discovered during processing
        if (pluginClass.startsWith("org.bithon.agent.plugin.")) {
            // Extract module name from plugin class package
            // Example: org.bithon.agent.plugin.redis.jedis2.Jedis2Plugin -> redis.jedis2
            String packagePart = pluginClass.substring("org.bithon.agent.plugin.".length());
            int lastDot = packagePart.lastIndexOf('.');
            if (lastDot > 0) {
                String moduleName = packagePart.substring(0, lastDot); // Remove class name, keep package path

                // Verify this module name matches our interceptors
                if (hasInterceptorsInModule(moduleName)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                             "Determined module name from plugin class [" + pluginClass + "]: " + moduleName);
                    return moduleName;
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                             "Plugin class [" + pluginClass + "] found but no interceptors match module [" + moduleName + "]. " +
                                                             "Interceptors found: " + interceptorTypes.keySet());
                    return null;
                }
            } else {
                // Single level package like org.bithon.agent.plugin.grpc.GrpcPlugin -> grpc
                String moduleName = packagePart;
                if (hasInterceptorsInModule(moduleName)) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.WARNING,
                                                             "Determined module name from plugin class [" + pluginClass + "]: " + moduleName);
                    return moduleName;
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                             "Plugin class [" + pluginClass + "] found but no interceptors match module [" + moduleName + "]. " +
                                                             "Interceptors found: " + interceptorTypes.keySet());
                    return null;
                }
            }
        }

        processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                 "Invalid plugin class name [" + pluginClass + "]. Plugin classes must be under org.bithon.agent.plugin package.");
        return null;
    }

    private boolean hasInterceptorsInModule(String moduleName) {
        if (moduleName == null) {
            return false;
        }
        String expectedPrefix = "org.bithon.agent.plugin." + moduleName + ".";
        return interceptorTypes.keySet().stream()
                               .anyMatch(className -> className.startsWith(expectedPrefix));
    }

    /**
     * Check if a class implements the IPlugin interface directly or indirectly
     */
    private boolean implementsIPlugin(TypeElement classElement, Types typeUtils) {
        // Get the IPlugin interface type
        TypeMirror iPluginType = getTypeMirror("org.bithon.agent.instrumentation.aop.interceptor.plugin.IPlugin");
        if (iPluginType == null) {
            // IPlugin interface not available in this compilation unit
            return false;
        }

        // Check direct interface implementation
        for (TypeMirror interfaceType : classElement.getInterfaces()) {
            if (typeUtils.isSameType(interfaceType, iPluginType)) {
                return true;
            }
        }

        // Check inheritance hierarchy for IPlugin implementation
        TypeElement currentElement = classElement;
        while (currentElement != null) {
            // Check interfaces of current class in hierarchy
            for (TypeMirror interfaceType : currentElement.getInterfaces()) {
                if (typeUtils.isSameType(interfaceType, iPluginType)) {
                    return true;
                }
            }

            // Move to superclass
            TypeMirror superclass = currentElement.getSuperclass();
            if (superclass.getKind().name().equals("NONE")) {
                break;
            }

            currentElement = (TypeElement) typeUtils.asElement(superclass);
        }

        return false;
    }


}
