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

package org.bithon.server.storage.datasource.spec;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.aggregator.ast.PostAggregatorExpressionBaseVisitor;
import org.bithon.server.datasource.aggregator.ast.PostAggregatorExpressionLexer;
import org.bithon.server.datasource.aggregator.ast.PostAggregatorExpressionParser;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.aggregator.NumberAggregator;
import org.bithon.server.storage.datasource.api.IQueryStageAggregator;
import org.bithon.server.storage.datasource.builtin.Function;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.typing.DoubleValueType;
import org.bithon.server.storage.datasource.typing.IValueType;
import org.bithon.server.storage.datasource.typing.LongValueType;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Locale;
import java.util.function.Supplier;

/**
 * @author frankchen
 */
public class PostAggregatorMetricSpec implements IMetricSpec {
    @Getter
    private final String name;

    @Getter
    private final String displayText;

    @Getter
    private final String unit;

    @Getter
    private final String expression;

    @Getter
    private final IValueType valueType;

    @Getter
    private final boolean visible;

    /**
     * runtime property
     */
    @JsonIgnore
    private final Supplier<PostAggregatorExpressionParser> parsers;

    @JsonIgnore
    private DataSourceSchema owner;

    @JsonCreator
    public PostAggregatorMetricSpec(@JsonProperty("name") @NotNull String name,
                                    @JsonProperty("displayText") @NotNull String displayText,
                                    @JsonProperty("unit") @NotNull String unit,
                                    @JsonProperty("expression") @NotNull String expression,
                                    @JsonProperty("valueType") @NotNull String valueType,
                                    @JsonProperty("visible") @Nullable Boolean visible) {
        this.name = name;
        this.displayText = displayText;
        this.unit = unit;
        this.expression = Preconditions.checkArgumentNotNull("expression", expression).trim();
        this.valueType = "long".equalsIgnoreCase(valueType) ? LongValueType.INSTANCE : DoubleValueType.INSTANCE;
        this.visible = visible == null ? true : visible;

        this.parsers = () -> {
            PostAggregatorExpressionLexer lexer = new PostAggregatorExpressionLexer(CharStreams.fromString(expression));
            lexer.getErrorListeners().clear();
            lexer.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new InvalidExpressionException(expression, charPositionInLine, msg);
                }
            });
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PostAggregatorExpressionParser parser = new PostAggregatorExpressionParser(tokens);
            parser.getErrorListeners().clear();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new InvalidExpressionException(expression, charPositionInLine, msg);
                }
            });

            /*
             * Verify the expression for fast failure
             */
            parser.parse().accept(new PostAggregatorExpressionBaseVisitor<Void>() {

                @Override
                public Void visitFieldNameExpression(PostAggregatorExpressionParser.FieldNameExpressionContext ctx) {
                    String fieldName = ctx.getText();
                    if (!owner.containsMetric(fieldName)) {
                        throw new IllegalStateException(String.format(Locale.ENGLISH,
                                                                      "[%s] in [%s] not found in schema",
                                                                      fieldName,
                                                                      name));
                    }
                    return null;
                }

                @Override
                public Void visitExpression(PostAggregatorExpressionParser.ExpressionContext ctx) {
                    if (ctx.getChild(0) instanceof PostAggregatorExpressionParser.FunctionNameExpressionContext) {
                        String functionName = ctx.getChild(0).getText();
                        Function function = Functions.getInstance().getFunction(functionName);
                        if (function == null) {
                            throw new IllegalStateException(StringUtils.format("function [%s] is not defined.", functionName));
                        }

                        List<PostAggregatorExpressionParser.ExpressionContext> argumentExpressions = ctx.getRuleContexts(PostAggregatorExpressionParser.ExpressionContext.class);
                        int argumentSize = argumentExpressions.size();
                        if (argumentSize != function.getParameters().size()) {
                            throw new IllegalStateException(StringUtils.format("In expression [%s], function [%s] has [%d] parameters, but only given [%d]",
                                                                               ctx.getText(),
                                                                               functionName,
                                                                               function.getParameters().size(),
                                                                               argumentSize));
                        }
                        for (int i = 0; i < argumentSize; i++) {
                            PostAggregatorExpressionParser.ExpressionContext argExpression = argumentExpressions.get(i);
                            function.getValidator().accept(i, argExpression);
                        }
                    }
                    return super.visitExpression(ctx);
                }
            });
            return parser;
        };
    }

    @JsonIgnore
    @Override
    public String getType() {
        return POST;
    }

    @Override
    public String getField() {
        return null;
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public NumberAggregator createAggregator() {
        return null;
    }

    @JsonIgnore
    @Override
    public IQueryStageAggregator getQueryAggregator() {
        return null;
    }

    @Override
    public void setOwner(DataSourceSchema dataSource) {
        this.owner = dataSource;
    }

    public void visitExpression(PostAggregatorExpressionVisitor visitor) {
        PostAggregatorExpressionParser parser = this.parsers.get();
        parser.reset();
        // TODO: dead-loop detection if expression contains THIS metricSpec
        parser.parse().accept(new PostAggregatorExpressionBaseVisitor<Void>() {
            @Override
            public Void visitExpression(PostAggregatorExpressionParser.ExpressionContext ctx) {
                switch (ctx.getChildCount()) {
                    case 1:
                        return visitChildren(ctx);
                    case 3: {
                        String operator = ctx.getChild(1).getText();
                        switch (operator) {
                            case "+":
                            case "-":
                            case "/":
                            case "*":
                                ctx.getChild(0).accept(this);
                                visitor.visitorOperator(operator);
                                ctx.getChild(2).accept(this);
                                return null;
                            default:
                                /*
                                 * Only one case：(A)
                                 */
                                visitor.beginSubExpression();
                                this.visit(ctx.getChild(1));
                                visitor.endSubExpression();
                                return null;
                        }
                    }
                    default:
                        ParseTree firstChild = ctx.getChild(0);
                        if (firstChild instanceof PostAggregatorExpressionParser.FunctionNameExpressionContext) {
                            // Processing function call
                            visitor.beginFunction(firstChild.getText());

                            // Processing function arguments
                            List<PostAggregatorExpressionParser.ExpressionContext> argumentExpressions = ctx.getRuleContexts(PostAggregatorExpressionParser.ExpressionContext.class);
                            int argumentSize = argumentExpressions.size();

                            for (int i = 0; i < argumentSize; i++) {
                                PostAggregatorExpressionParser.ExpressionContext argExpression = argumentExpressions.get(i);
                                visitor.beginFunctionArgument(i, argumentSize);
                                this.visit(argExpression);
                                visitor.endFunctionArgument(i, argumentSize);
                            }

                            visitor.endFunction();

                            return null;
                        } else {
                            // no such case
                            throw new IllegalStateException("ChildCount is "
                                                            + ctx.getChildCount()
                                                            + ", Text="
                                                            + ctx.getText());
                        }
                }
            }

            @Override
            public Void visitTerminal(TerminalNode node) {
                switch (node.getSymbol().getType()) {
                    case Token.EOF:
                    case PostAggregatorExpressionParser.COMMA:
                        return null;
                    case PostAggregatorExpressionParser.NUMBER:
                        visitor.visitConstant(node.getText());
                        return null;
                    case PostAggregatorExpressionParser.ID:
                        visitor.visitMetric(owner.getMetricSpecByName(node.getText()));
                        return null;
                    default:
                        throw new IllegalStateException("Terminal Node Type:"
                                                        + node.getSymbol().getType()
                                                        + ", Input Expression:"
                                                        + expression);
                }
            }

            @Override
            public Void visitVariableExpression(PostAggregatorExpressionParser.VariableExpressionContext ctx) {
                visitor.visitVariable(ctx.getChild(1).getText());
                return null;
            }
        });
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PostAggregatorMetricSpec) {
            return this.name.equals(((PostAggregatorMetricSpec) obj).name);
        } else {
            return false;
        }
    }
}
