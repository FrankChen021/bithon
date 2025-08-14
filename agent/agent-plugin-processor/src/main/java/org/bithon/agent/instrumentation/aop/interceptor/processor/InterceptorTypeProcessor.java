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
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
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
 * @date 2025/08/14 13:30
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
        if (roundEnv.processingOver()) {
            // Generate the registry class on the final round
            if (!processed) {
                generatePluginMetadata();
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

                // Check if this is a plugin class (ends with "Plugin" and implements IPlugin)
                if (className.endsWith("Plugin") && implementsIPlugin(classElement, typeUtils)) {
                    if (pluginClass == null) {
                        pluginClass = className;
                        processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found plugin class: " + className);
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
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Found interceptor: " + className + " -> " + type);
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

        // Skip abstract classes - they cannot be instantiated as interceptors
        if (classElement.getModifiers().contains(Modifier.ABSTRACT)) {
            return null;
        }

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

    private void generatePluginMetadata() {
        if (pluginClass == null && interceptorTypes.isEmpty()) {
            // No plugin class or interceptors found, nothing to generate
            return;
        } else {
            if (pluginClass == null) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                         "No plugin class found, but interceptors found.");
                return;
            }
            if (interceptorTypes.isEmpty()) {
                processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                         "No interceptors found, but plugin class found.");
                return;
            }
        }

        try {
            String metadataFile = "META-INF/bithon/" + this.pluginClass + ".meta";

            // Create the properties file in the META-INF/bithon directory  
            FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", metadataFile);
            try (Writer writer = file.openWriter()) {
                // Write plugin class section
                writer.write("[" + this.pluginClass + "]\n");

                // Generate the interceptor entries under the plugin section
                for (Map.Entry<String, InterceptorType> entry : interceptorTypes.entrySet()) {
                    writer.write(entry.getKey() + "=" + entry.getValue() + "\n");
                }

                // Extra space so that two plugins can be separated by a blank line in the final merged file
                writer.write("\n");
            }

            processingEnv.getMessager().printMessage(Diagnostic.Kind.NOTE, "Generated " + metadataFile + " with " + interceptorTypes.size() + " interceptors");

        } catch (IOException e) {
            processingEnv.getMessager().printMessage(Diagnostic.Kind.ERROR,
                                                     "Failed to generate plugin metadata: " + e.getMessage());
        }
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
