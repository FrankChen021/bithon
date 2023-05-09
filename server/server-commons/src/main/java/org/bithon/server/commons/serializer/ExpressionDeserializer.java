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

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.bithon.component.commons.expression.BinaryExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * {@link IExpression} is used at both server and agent side, since the server and the agent use different jackson to deal with deserialization,
 * we can depend on jackson's own deserialization mechanism but instead, we deserialize the object manually.
 *
 * @author frank.chen021@outlook.com
 * @date 2023/4/7 22:12
 */
public class ExpressionDeserializer extends JsonDeserializer<IExpression> {

    @Override
    public Class<?> handledType() {
        return IExpression.class;
    }

    @Override
    public IExpression deserialize(JsonParser jsonParser,
                                   DeserializationContext ctx) throws IOException {

        JsonNode jsonNode = jsonParser.getCodec().readTree(jsonParser);
        return Expression.deserialize(jsonNode);
    }

    static class Expression {
        static IExpression deserialize(JsonNode jsonNode) throws IOException {
            JsonNode typeNode = jsonNode.get("type");
            String type = typeNode.asText();
            switch (type) {
                case "binary":
                    return BinaryExpressionDeserializer.deserialize(jsonNode);
                case "literal":
                    return LiteralExpressionDeserializer.deserialize(jsonNode);
                case "logical":
                    return LogicalExpressionDeserializer.deserialize(jsonNode);
                case "identifier":
                    return IdentifierExpressionDeserializer.deserialize(jsonNode);
                case "expressionList":
                    return ExpressionListDeserializer.deserialize(jsonNode);
                default:
                    throw new RuntimeException("Unknown type " + type);
            }
        }
    }

    /**
     * {@link org.bithon.component.commons.expression.BinaryExpression}
     */
    static class BinaryExpressionDeserializer {
        static IExpression deserialize(JsonNode jsonNode) throws IOException {
            JsonNode operator = jsonNode.get("operator");
            if (operator == null) {
                throw new RuntimeException("Missing 'operator' field");
            }

            JsonNode left = jsonNode.get("left");
            JsonNode right = jsonNode.get("right");
            return BinaryExpression.create(operator.asText(),
                                           Expression.deserialize(left),
                                           Expression.deserialize(right));
        }
    }

    /**
     * {@link org.bithon.component.commons.expression.LogicalExpression}
     */
    static class LogicalExpressionDeserializer {
        static IExpression deserialize(JsonNode jsonNode) throws IOException {

            JsonNode operator = jsonNode.get("operator");
            if (operator == null) {
                throw new RuntimeException("Missing 'operator' field");
            }

            JsonNode operands = jsonNode.get("operands");
            if (operands == null) {
                throw new RuntimeException("Missing 'operands' field");
            }
            if (!operands.isArray()) {
                throw new RuntimeException("'operands' must be type of array.");
            }

            List<IExpression> expressionList = new ArrayList<>();
            for (JsonNode operand : operands) {
                expressionList.add(Expression.deserialize(operand));
            }

            return LogicalExpression.create(operator.asText(), expressionList);
        }
    }

    /**
     * {@link org.bithon.component.commons.expression.LiteralExpression}
     */
    static class LiteralExpressionDeserializer {
        static IExpression deserialize(JsonNode jsonNode) {

            JsonNode valueNode = jsonNode.get("value");
            if (valueNode.isTextual()) {
                return new LiteralExpression(valueNode.asText());
            }

            if (valueNode.isLong()) {
                return new LiteralExpression(valueNode.asLong());
            }

            if (valueNode.isInt()) {
                return new LiteralExpression(valueNode.asInt());
            }

            if (valueNode.isBoolean()) {
                return new LiteralExpression(valueNode.asBoolean());
            }
            throw new UnsupportedOperationException("value is not type of any [String/Long/Int/Boolean]");
        }
    }

    /**
     * {@link org.bithon.component.commons.expression.IdentifierExpression}
     */
    static class IdentifierExpressionDeserializer {
        static IExpression deserialize(JsonNode jsonNode) {

            JsonNode identifierNode = jsonNode.get("identifier");
            if (identifierNode == null) {
                throw new RuntimeException("Missing 'identifier' field");
            }
            return new IdentifierExpression(identifierNode.asText());
        }
    }

    static class ExpressionListDeserializer {
        static IExpression deserialize(JsonNode jsonNode) throws IOException {

            JsonNode expressionList = jsonNode.get("expressionList");
            if (expressionList == null) {
                throw new RuntimeException("Missing 'expressionList' field");
            }
            if (!(expressionList instanceof ArrayNode)) {
                throw new RuntimeException("expressionList should be an array.");
            }

            List<IExpression> exprList = new ArrayList<>();
            for (JsonNode node : expressionList) {
                exprList.add(Expression.deserialize(node));
            }
            return new ExpressionList(exprList);
        }
    }
}
