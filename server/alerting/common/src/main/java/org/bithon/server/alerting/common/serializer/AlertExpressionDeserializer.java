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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import org.bithon.server.alerting.common.model.AlertExpression;
import org.bithon.server.alerting.common.parser.AlertExpressionASTParser;

import java.io.IOException;

/**
 * Notification API might be remote service, we have to deserialize the expression from request
 *
 * @author frank.chen021@outlook.com
 * @date 4/12/24 5:29 pm
 */
public class AlertExpressionDeserializer extends JsonDeserializer<AlertExpression> {
    @Override
    public AlertExpression deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.getCodec().readTree(p);

        // Use AST parser to parse expression instead of deserializing from JSON
        // This is because it's simpler and more reliable
        JsonNode exprNode = node.get("expressionText");
        AlertExpression expr = (AlertExpression) AlertExpressionASTParser.parse(exprNode.asText());

        JsonNode id = node.get("id");
        expr.setId(id.asText());
        return expr;
    }

    @Override
    public Class<?> handledType() {
        return AlertExpression.class;
    }
}
