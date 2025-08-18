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


import com.google.common.collect.ImmutableList;
import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 18/8/25 11:48 am
 */
public class MetadataProcessorTest {

    @Test
    public void testMetadataGeneration() {
        String src = "package org.bithon.agent.configuration.test;\n" +
                     "import org.bithon.agent.configuration.annotation.ConfigurationProperties;\n" +
                     "public class Demo {\n" +
                     "  @ConfigurationProperties(path=\"demo.config\")\n" +
                     "  public static class Cfg {\n" +
                     "     private String name = \"abc\";\n" +
                     "     public String getName() { return name; }\n" +
                     "     private int size = 10;\n" +
                     "     public int getSize() { return size; }\n" +
                     "  }\n" +
                     "}\n";
        CompiledConfigResult result = compileConfigurationPropertyClass("org.bithon.agent.configuration.test.Demo", src);

        Map<String, PropertyMetadata> props = result.properties;
        {
            PropertyMetadata metadata = props.get("demo.config.name");
            Assertions.assertNotNull(metadata);
            Assertions.assertEquals("demo.config.name", metadata.getPath());
            Assertions.assertEquals("String", metadata.getType());
            Assertions.assertTrue(metadata.isDynamic());
        }
        {
            PropertyMetadata metadata = props.get("demo.config.size");
            Assertions.assertNotNull(metadata);
            Assertions.assertEquals("demo.config.size", metadata.getPath());
            Assertions.assertEquals("int", metadata.getType());
            Assertions.assertTrue(metadata.isDynamic());
        }
    }

    public static class CompiledConfigResult {
        public final Map<String, Class<?>> compiledClasses;
        public final Map<String, PropertyMetadata> properties;

        public CompiledConfigResult(Map<String, Class<?>> compiledClasses, Map<String, PropertyMetadata> properties) {
            this.compiledClasses = compiledClasses;
            this.properties = properties;
        }
    }

    private static class InMemoryClassLoader extends ClassLoader {
        private final Map<String, byte[]> classBytesMap;

        public InMemoryClassLoader(Map<String, byte[]> classBytesMap) {
            super(InMemoryClassLoader.class.getClassLoader());
            this.classBytesMap = classBytesMap;
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException {
            byte[] bytes = classBytesMap.get(name);
            if (bytes == null) {
                throw new ClassNotFoundException(name);
            }
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    public static CompiledConfigResult compileConfigurationPropertyClass(String className, String code) {
        Compilation compilation = Compiler.javac()
                                          .withProcessors(new MetadataProcessor())
                                          .compile(JavaFileObjects.forSourceString(className, code));
        ImmutableList<Diagnostic<? extends JavaFileObject>> errors = compilation.errors();
        if (!errors.isEmpty()) {
            throw new RuntimeException(errors.toString());
        }

        // Collect all class files and their names and bytes
        Map<String, byte[]> classBytesMap = new HashMap<>();
        for (FileObject classFile : compilation.generatedFiles()) {
            String name = classFile.getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            int idx = name.indexOf("/org/bithon/");
            if (idx == -1) {
                continue;
            }
            String classPath = name.substring(idx + 1, name.length() - ".class".length());
            String compiledClassName = classPath.replace('/', '.');
            byte[] classBytes;
            try (InputStream is = classFile.openInputStream()) {
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                byte[] temp = new byte[4096];
                int read;
                while ((read = is.read(temp)) != -1) {
                    buffer.write(temp, 0, read);
                }
                classBytes = buffer.toByteArray();
            } catch (IOException e) {
                throw new RuntimeException("Failed to read class bytes", e);
            }
            classBytesMap.put(compiledClassName, classBytes);
        }

        // Load all generated classes
        InMemoryClassLoader loader = new InMemoryClassLoader(classBytesMap);
        Map<String, Class<?>> compiledClasses = new HashMap<>();
        for (String compiledClassName : classBytesMap.keySet()) {
            try {
                Class<?> clazz = loader.loadClass(compiledClassName);
                compiledClasses.put(compiledClassName, clazz);
            } catch (ClassNotFoundException e) {
                throw new RuntimeException("Failed to load class " + compiledClassName, e);
            }
        }

        // Find the .meta file
        FileObject metaFile = compilation.generatedFiles()
                                         .stream()
                                         .filter(f -> f.getName().endsWith(".meta") && f.toUri().toString().contains("META-INF/bithon/configuration"))
                                         .findFirst()
                                         .orElseThrow(() -> new IllegalStateException("Metadata file not generated"));

        // Deserialize PropertyMetadata list and convert to map
        Map<String, PropertyMetadata> properties;
        try {
            ObjectMapper om = new ObjectMapper();
            List<PropertyMetadata> propertyList = om.readValue(om.createParser(new InputStreamReader(metaFile.openInputStream(), StandardCharsets.UTF_8)),
                                                               new TypeReference<List<PropertyMetadata>>() {
                                                               });
            properties = propertyList.stream()
                                     .collect(Collectors.toMap(PropertyMetadata::getPath, p -> p));
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize PropertyMetadata from meta file", e);
        }
        return new CompiledConfigResult(compiledClasses, properties);
    }
}
