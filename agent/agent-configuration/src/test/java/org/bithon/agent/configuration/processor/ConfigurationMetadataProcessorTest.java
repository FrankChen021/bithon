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


import com.google.testing.compile.Compilation;
import com.google.testing.compile.Compiler;
import com.google.testing.compile.JavaFileObjects;
import org.bithon.agent.configuration.metadata.PropertyMetadata;
import org.bithon.shaded.com.fasterxml.jackson.core.type.TypeReference;
import org.bithon.shaded.com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.tools.FileObject;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 18/8/25 11:48 am
 */
public class ConfigurationMetadataProcessorTest {

    @Test
    public void testMetadataGeneration() throws Exception {
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
        FileObject meta = compileConfigurationPropertyClass("org.bithon.agent.configuration.test.Demo", src);

        ObjectMapper om = new ObjectMapper();
        List<PropertyMetadata> props = om.readValue(om.createParser(new InputStreamReader(meta.openInputStream(), StandardCharsets.UTF_8)),
                                                    new TypeReference<List<PropertyMetadata>>() {
                                                    });

        Assertions.assertTrue(props.stream().anyMatch(p ->
                                                          p.getPath().equals("demo.config.name")
                                                          && p.isDynamic()
                                                          && "String".equals(p.getType())));
        Assertions.assertTrue(props.stream().anyMatch(p ->
                                                          p.getPath().equals("demo.config.size")
                                                          && p.isDynamic()
                                                          && "int".equals(p.getType())));
    }

    public static FileObject compileConfigurationPropertyClass(String fqn, String code) {
        Compilation compilation = Compiler.javac()
                                          .withProcessors(new ConfigurationMetadataProcessor())
                                          .compile(JavaFileObjects.forSourceString("org.bithon.agent.configuration.test.Demo", code));
        Assertions.assertFalse(compilation.errors().stream().findAny().isPresent(), "Compilation failed");
        return compilation.generatedFiles()
                          .stream()
                          .filter(f -> f.getName().endsWith(".meta") && f.toUri().toString().contains("META-INF/bithon/configuration"))
                          .findFirst()
                          .orElseThrow(() -> new IllegalStateException("Metadata file not generated"));
    }
}
