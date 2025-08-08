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

package org.bithon.agent.controller.config;

import org.bithon.agent.configuration.ConfigurationManager;
import org.bithon.agent.configuration.ConfigurationProperties;
import org.bithon.agent.controller.cmd.IAgentCommand;
import org.bithon.agent.instrumentation.loader.JarClassLoader;
import org.bithon.agent.instrumentation.loader.PluginClassLoader;
import org.bithon.agent.instrumentation.logging.LoggerFactory;
import org.bithon.agent.rpc.brpc.cmd.IConfigurationCommand;
import org.bithon.shaded.net.bytebuddy.jar.asm.AnnotationVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassReader;
import org.bithon.shaded.net.bytebuddy.jar.asm.ClassVisitor;
import org.bithon.shaded.net.bytebuddy.jar.asm.Opcodes;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/1/7 17:33
 */
public class ConfigurationCommandImpl implements IConfigurationCommand, IAgentCommand {
    @Override
    public List<String> getConfiguration(String format, boolean prettyFormat) {
        return Collections.singletonList(ConfigurationManager.getInstance()
                                                             .getActiveConfiguration(format.toLowerCase(Locale.ENGLISH), prettyFormat));
    }

    @Override
    public List<ConfigurationMetadata> getConfigurationMetadata() {
        List<ConfigurationMetadata> metadataList = new ArrayList<>();
        
        try {
            // Get all classes from PluginClassLoader that are annotated with @ConfigurationProperties
            ClassLoader pluginClassLoader = PluginClassLoader.getClassLoader();
            if (pluginClassLoader == null) {
                return metadataList;
            }
            
            // Find all configuration classes
            Set<Class<?>> configClasses = findConfigurationClasses(pluginClassLoader);
            
            for (Class<?> configClass : configClasses) {
                processConfigurationClass(configClass, metadataList);
            }
            
        } catch (Exception e) {
            // Log the error but return partial results
            LoggerFactory.getLogger(ConfigurationCommandImpl.class)
                         .warn("Failed to scan configuration metadata: " + e.getMessage(), e);
        }
        
        return metadataList;
    }
    
    private Set<Class<?>> findConfigurationClasses(ClassLoader classLoader) {
        Set<Class<?>> configClasses = new HashSet<>();
        
        try {
            // Scan all classes in the PluginClassLoader similar to how PluginResolver scans for plugins
            if (classLoader instanceof JarClassLoader) {
                JarClassLoader jarClassLoader = (JarClassLoader) classLoader;
                scanJarFilesForConfigClasses(jarClassLoader, configClasses);
            }
            
        } catch (Exception e) {
            LoggerFactory.getLogger(ConfigurationCommandImpl.class)
                         .warn("Error scanning for configuration classes: " + e.getMessage(), e);
        }
        
        return configClasses;
    }
    
    private void scanJarFilesForConfigClasses(JarClassLoader jarClassLoader, Set<Class<?>> configClasses) {
        // Scan all JAR files in the PluginClassLoader, similar to PluginResolver.loadPlugins()
        for (JarFile jarFile : jarClassLoader.getJars()) {
            jarFile.stream()
                   .filter(jarEntry -> jarEntry.getName().endsWith(".class"))
                   .filter(jarEntry -> jarEntry.getName().startsWith("org/bithon/"))
                   .forEach(jarEntry -> {
                       try {
                           // Use ASM to check for @ConfigurationProperties annotation without loading the class
                           if (hasConfigurationPropertiesAnnotation(jarEntry, jarClassLoader)) {
                               String className = jarEntry.getName()
                                                          .substring(0, jarEntry.getName().length() - ".class".length())
                                                          .replace('/', '.');
                               
                               // Only load the class if we know it has the annotation
                               Class<?> clazz = Class.forName(className, false, jarClassLoader);
                               configClasses.add(clazz);
                           }
                       } catch (ClassNotFoundException | NoClassDefFoundError e) {
                           // Skip classes that can't be loaded - this is normal for classes with missing dependencies
                       } catch (Exception e) {
                           // Log other unexpected errors but continue
                           LoggerFactory.getLogger(ConfigurationCommandImpl.class)
                                        .warn("Failed to analyze class " + jarEntry.getName() + ": " + e.getMessage());
                       }
                   });
        }
    }
    
    /**
     * Check if a class file has @ConfigurationProperties annotation using ASM similar to InterceptorTypeResolver
     */
    private boolean hasConfigurationPropertiesAnnotation(JarEntry jarEntry, JarClassLoader classLoader) {
        try (InputStream is = classLoader.getResourceAsStream(jarEntry.getName())) {
            if (is == null) {
                return false;
            }
            
            ClassReader classReader = new ClassReader(is);
            ConfigurationPropertiesAnnotationVisitor visitor = new ConfigurationPropertiesAnnotationVisitor();
            classReader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            
            return visitor.hasConfigurationPropertiesAnnotation();
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * ASM ClassVisitor to detect @ConfigurationProperties annotation, similar to SuperNameExtractor in InterceptorTypeResolver
     */
    static class ConfigurationPropertiesAnnotationVisitor extends ClassVisitor {
        private boolean hasConfigurationPropertiesAnnotation = false;
        private static final String CONFIGURATION_PROPERTIES_DESC = "Lorg/bithon/agent/configuration/ConfigurationProperties;";
        
        protected ConfigurationPropertiesAnnotationVisitor() {
            super(Opcodes.ASM9);
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            if (CONFIGURATION_PROPERTIES_DESC.equals(descriptor)) {
                hasConfigurationPropertiesAnnotation = true;
            }
            return null;
        }
        
        public boolean hasConfigurationPropertiesAnnotation() {
            return hasConfigurationPropertiesAnnotation;
        }
    }
    
    private void processConfigurationClass(Class<?> configClass, List<ConfigurationMetadata> metadataList) {
        ConfigurationProperties configProps = configClass.getAnnotation(ConfigurationProperties.class);
        
        if (configProps == null) {
            return;
        }
        
        String basePath = configProps.path();
        
        // Process all non-static fields in the class
        Field[] fields = configClass.getDeclaredFields();
        for (Field field : fields) {
            // Skip static fields and synthetic fields
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            
            // For now, include all instance fields since PropertyDescriptor is not commonly used
            // In the future, when PropertyDescriptor is more widely adopted, we can filter by it
            ConfigurationMetadata metadata = new ConfigurationMetadata();
            
            // Build the full property path
            String fieldPath = basePath + "." + field.getName();
            metadata.setPath(fieldPath);
            
            // Set field type
            metadata.setType(field.getType().getSimpleName());
            
            // Try to get description from PropertyDescriptor if available
            try {
                @SuppressWarnings("unchecked")
                Class<? extends java.lang.annotation.Annotation> propertyDescriptorClass = 
                    (Class<? extends java.lang.annotation.Annotation>) Class.forName("org.bithon.agent.configuration.PropertyDescriptor");
                java.lang.annotation.Annotation propertyDesc = field.getAnnotation(propertyDescriptorClass);
                if (propertyDesc != null) {
                    // Use reflection to get description
                    String description = (String) propertyDescriptorClass.getMethod("description").invoke(propertyDesc);
                    metadata.setDescription(description);
                } else {
                    metadata.setDescription("Configuration property: " + field.getName());
                }
            } catch (Exception e) {
                // PropertyDescriptor not available or no annotation, use field name as fallback
                metadata.setDescription("Configuration property: " + field.getName());
            }
            
            // Try to get default value
            try {
                Object defaultInstance = createDefaultInstance(configClass);
                if (defaultInstance != null) {
                    field.setAccessible(true);
                    Object defaultValue = field.get(defaultInstance);
                    if (defaultValue != null) {
                        metadata.setDefaultValue(defaultValue.toString());
                    }
                }
            } catch (Exception e) {
                // If we can't get default value, leave it null
                LoggerFactory.getLogger(ConfigurationCommandImpl.class)
                             .warn("Could not extract default value for field " + field.getName() + 
                                  " in class " + configClass.getName() + ": " + e.getMessage());
            }
            
            metadataList.add(metadata);
        }
    }
    
    private Object createDefaultInstance(Class<?> clazz) {
        try {
            // Try to create instance using default constructor
            return clazz.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            return null;
        }
    }
}
