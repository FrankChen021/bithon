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

package org.bithon.server.alerting.common.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;
import org.bithon.server.commons.serializer.HumanReadableDurationDeserializer;
import org.bithon.server.commons.serializer.HumanReadableDurationSerializer;
import org.bithon.server.commons.serializer.HumanReadablePercentageSerializer;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

/**
 * @author Frank Chen
 * @date 15/2/24 5:32 pm
 */
public class AlertExpressionSerializerTest {

    @Test
    public void testJsonSerialization() throws JsonProcessingException {
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName='a', instance='b'})[5m] > 1%[-7m]");

        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder()
            .serializers(new AlertExpressionSerializer(), new HumanReadablePercentageSerializer(), new HumanReadableDurationSerializer())
            .indentOutput(true)
            .build();

        String val = objectMapper.writeValueAsString(expression);
        JsonNode tree = objectMapper.readTree(val);

        Assert.assertEquals("1", tree.get("id").asText());
        Assert.assertEquals("avg(jvm-metrics.cpu{appName = \"a\", instance = \"b\"})[5m] > 1%[-7m]", tree.get("expressionText").asText());
        Assert.assertEquals("(appName = 'a' AND instance = 'b')", tree.get("where").asText());
    }

    @Test
    public void testEscapeLabel() throws JsonProcessingException {
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName='ab\"cd', instance='ab\\'cd'})[5m] > 1%[-7m]");

        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder()
            .serializers(new AlertExpressionSerializer(), new HumanReadablePercentageSerializer(), new HumanReadableDurationSerializer())
            .indentOutput(true)
            .build();

        String val = objectMapper.writeValueAsString(expression);
        JsonNode tree = objectMapper.readTree(val);

        Assert.assertEquals("1", tree.get("id").asText());

        // appName should be escaped as ab\"cd
        Assert.assertEquals("avg(jvm-metrics.cpu{appName = \"ab\\\"cd\", instance = \"ab'cd\"})[5m] > 1%[-7m]", tree.get("expressionText").asText());
        Assert.assertEquals("(appName = 'ab\"cd' AND instance = 'ab\\'cd')", tree.get("where").asText());
    }

    @Test
    public void testJsonSerialization_NoExpectedWindow() throws JsonProcessingException {
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName='a', instance='b'})[5m] > 1");

        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder()
            .serializers(new AlertExpressionSerializer(), new HumanReadableDurationSerializer())
            .indentOutput(true)
            .build();

        String val = objectMapper.writeValueAsString(expression);
        JsonNode tree = objectMapper.readTree(val);

        Assert.assertEquals("1", tree.get("id").asText());
        Assert.assertEquals("avg(jvm-metrics.cpu{appName = \"a\", instance = \"b\"})[5m] > 1", tree.get("expressionText").asText());
        Assert.assertEquals("(appName = 'a' AND instance = 'b')", tree.get("where").asText());
    }

    @Test
    public void test_Deserialization() throws JsonProcessingException {
        AlertExpression expression = (AlertExpression) AlertExpressionASTParser.parse("avg(jvm-metrics.cpu{appName='a', instance='b'})[5m] > 1");

        ObjectMapper objectMapper = new Jackson2ObjectMapperBuilder()
            .serializers(new AlertExpressionSerializer(), new HumanReadableDurationSerializer())
            .deserializers(new AlertExpressionDeserializer(), new HumanReadableDurationDeserializer())
            .indentOutput(true)
            .build();

        String val = objectMapper.writeValueAsString(expression);
        AlertExpression deserialized = objectMapper.readValue(val, AlertExpression.class);
        Assert.assertEquals(expression.getId(), deserialized.getId());
        Assert.assertEquals(expression.getMetricExpression().getFrom(), deserialized.getMetricExpression().getFrom());
    }
}
