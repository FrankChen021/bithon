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

package org.bithon.server.commons.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


/**
 * @author frank.chen021@outlook.com
 * @date 2023/4/8 11:05
 */
public class ExpressionDeserializerTest {

    @Test
    public void testSerializationAndDeserialization() throws JsonProcessingException {
        ObjectMapper om = new ObjectMapper();
        SimpleModule m = new SimpleModule();
        m.addDeserializer(IExpression.class, new ExpressionDeserializer());
        om.registerModule(m);

        IExpression expression = new LogicalExpression.AND(new ComparisonExpression.GT(LiteralExpression.ofLong(1),
                                                                                       LiteralExpression.ofLong(2)),
                                                           LiteralExpression.ofBoolean(true),
                                                           new IdentifierExpression("a"),
                                                           new ConditionalExpression.In(new IdentifierExpression("a"),
                                                                                        new ExpressionList(LiteralExpression.ofLong(1),
                                                                                                           LiteralExpression.ofLong(2)))
        );

        String jsonText = om.writeValueAsString(expression);

        // Deserialize and Serialized again
        IExpression deserialized = om.readValue(jsonText, IExpression.class);
        String jsonText2 = om.writeValueAsString(deserialized);

        Assertions.assertEquals(jsonText, jsonText2);
    }

}
