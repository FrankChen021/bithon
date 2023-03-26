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

package org.bithon.server.storage.datasource.query.parser;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ast.FieldExpressionBaseVisitor;
import org.bithon.server.datasource.ast.FieldExpressionLexer;
import org.bithon.server.datasource.ast.FieldExpressionParser;
import org.bithon.server.storage.datasource.builtin.Function;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.spec.InvalidExpressionException;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 2022/11/3 21:03
 */
public class FieldExpressionParserImpl {

    private final FieldExpressionParser parser;

    public FieldExpressionParserImpl(FieldExpressionParser parser) {
        this.parser = parser;
    }

    public void visit(FieldExpressionVisitorAdaptor visitor) {
        parser.reset();

        // TODO: dead-loop detection if expression contains THIS metricSpec
        parser.parse().accept(new FieldExpressionBaseVisitor<Void>() {
            @Override
            public Void visitFieldExpression(FieldExpressionParser.FieldExpressionContext ctx) {
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
                        // no such case
                        throw new IllegalStateException("ChildCount is "
                                                        + ctx.getChildCount()
                                                        + ", Text="
                                                        + ctx.getText());
                }
            }

            @Override
            public Void visitFieldNameExpression(FieldExpressionParser.FieldNameExpressionContext ctx) {
                visitor.visitField(ctx.getText());
                return null;
            }

            @Override
            public Void visitFunctionExpression(FieldExpressionParser.FunctionExpressionContext ctx) {
                ParseTree functionName = ctx.getChild(0);

                // Processing function call
                visitor.beginFunction(functionName.getText());

                // Processing function arguments
                List<FieldExpressionParser.FieldExpressionContext> argumentExpressions = ctx.getRuleContexts(FieldExpressionParser.FieldExpressionContext.class);
                int argumentSize = argumentExpressions.size();

                for (int i = 0; i < argumentSize; i++) {
                    FieldExpressionParser.FieldExpressionContext argExpression = argumentExpressions.get(i);
                    visitor.beginFunctionArgument(i, argumentSize);
                    this.visit(argExpression);
                    visitor.endFunctionArgument(i, argumentSize);
                }

                visitor.endFunction();

                return null;
            }

            @Override
            public Void visitTerminal(TerminalNode node) {
                switch (node.getSymbol().getType()) {
                    case Token.EOF:
                    case FieldExpressionParser.COMMA:
                        return null;
                    case FieldExpressionParser.NUMBER:
                        visitor.visitConstant(node.getText());
                        return null;
                    default:
                        throw new IllegalStateException("Terminal Node Type:"
                                                        + node.getSymbol().getType()
                                                        + ", Input Expression:"
                                                        + node.getText());
                }
            }

            @Override
            public Void visitVariableExpression(FieldExpressionParser.VariableExpressionContext ctx) {
                visitor.visitVariable(ctx.getChild(1).getText());
                return null;
            }
        });
    }


    public static FieldExpressionParserImpl create(String expression) {
        FieldExpressionLexer lexer = new FieldExpressionLexer(CharStreams.fromString(expression));
        lexer.getErrorListeners().clear();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new InvalidExpressionException(expression, line, charPositionInLine, msg);
            }
        });
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        FieldExpressionParser parser = new FieldExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new InvalidExpressionException(expression, line, charPositionInLine, msg);
            }
        });

        /*
         * Verify the expression for fast failure
         */
        parser.parse().accept(new FieldExpressionBaseVisitor<Void>() {
            @Override
            public Void visitFunctionExpression(FieldExpressionParser.FunctionExpressionContext ctx) {
                String functionName = ctx.getChild(0).getText();
                Function function = Functions.getInstance().getFunction(functionName);
                if (function == null) {
                    throw new IllegalStateException(StringUtils.format("function [%s] is not defined.", functionName));
                }

                List<FieldExpressionParser.FieldExpressionContext> argumentExpressions = ctx.getRuleContexts(FieldExpressionParser.FieldExpressionContext.class);
                int argumentSize = argumentExpressions.size();
                if (argumentSize != function.getParameters().size()) {
                    throw new IllegalStateException(StringUtils.format("In expression [%s], function [%s] has [%d] parameters, but only given [%d]",
                                                                       ctx.getText(),
                                                                       functionName,
                                                                       function.getParameters().size(),
                                                                       argumentSize));
                }
                for (int i = 0; i < argumentSize; i++) {
                    FieldExpressionParser.FieldExpressionContext argExpression = argumentExpressions.get(i);
                    function.getValidator().accept(i, argExpression);
                }

                return null;
            }
        });

        return new FieldExpressionParserImpl(parser);
    }
}
