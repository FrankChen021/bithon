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

package org.bithon.server.storage.common.expression;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.CollectionExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.FieldExpression;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.datasource.ast.ExpressionBaseVisitor;
import org.bithon.server.datasource.ast.ExpressionLexer;
import org.bithon.server.datasource.ast.ExpressionParser;
import org.bithon.server.storage.datasource.builtin.IFunctionProvider;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Frank Chen
 * @date 1/7/23 8:37 pm
 */
public class ExpressionASTBuilder extends ExpressionBaseVisitor<IExpression> {

    public static IExpression build(String expression, IFunctionProvider functionProvider) {
        ExpressionLexer lexer = new ExpressionLexer(CharStreams.fromString(expression));
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
        ExpressionParser parser = new ExpressionParser(tokens);
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

        return parser.parse()
                     .expression()
                     .accept(new ExpressionASTBuilder(functionProvider));
    }

    private final IFunctionProvider functionProvider;

    private ExpressionASTBuilder(IFunctionProvider functionProvider) {
        this.functionProvider = functionProvider;
    }

    /**
     * a AND b AND c
     *
     * @param ctx the parse tree
     */
    @Override
    public IExpression visitLogicExpression(ExpressionParser.LogicExpressionContext ctx) {
        IExpression left = ctx.getChild(0).accept(this);

        for (int i = 1; i < ctx.children.size(); i += 2) {
            TerminalNode op = (TerminalNode) ctx.getChild(i);

            IExpression right = ctx.getChild(i + 1).accept(this);
            switch (op.getSymbol().getType()) {
                case ExpressionLexer.AND:
                    left = new LogicalExpression.AND(left, right);
                    break;

                case ExpressionLexer.OR:
                    left = new LogicalExpression.OR(left, right);
                    break;

                default:
                    throw new RuntimeException();
            }
        }
        return left;
    }

    @Override
    public IExpression visitBinaryExpression(ExpressionParser.BinaryExpressionContext ctx) {
        // There's only one TerminalNode in binaryExpression root definition, use index 0 to get that node
        TerminalNode op = ctx.getChild(TerminalNode.class, 0);

        switch (op.getSymbol().getType()) {
            case ExpressionLexer.ADD:
                return new ArithmeticExpression.Add(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.SUB:
                return new ArithmeticExpression.Sub(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.MUL:
                return new ArithmeticExpression.Mul(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.DIV:
                return new ArithmeticExpression.Div(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.LT:
                return new ComparisonExpression.LT(ctx.getChild(0).accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.LTE:
                return new ComparisonExpression.LTE(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.GT:
                return new ComparisonExpression.GT(ctx.getChild(0).accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.GTE:
                return new ComparisonExpression.GTE(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.NE:
                return new ComparisonExpression.NE(ctx.getChild(0).accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.EQ:
                return new ComparisonExpression.EQ(ctx.getChild(0).accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.LIKE:
                return new ComparisonExpression.LIKE(ctx.getChild(0).accept(this),
                                                     ctx.getChild(2).accept(this));

            case ExpressionLexer.NOT_LIKE:
                return new LogicalExpression.NOT(
                    new ComparisonExpression.LIKE(ctx.getChild(0).accept(this),
                                                  ctx.getChild(2).accept(this))
                );

            default:
                throw new RuntimeException();
        }
    }

    @Override
    public IExpression visitInExpression(ExpressionParser.InExpressionContext ctx) {
        IExpression left = ctx.subExpression().accept(this);

        List<ExpressionParser.LiteralExpressionImplContext> expressions = ctx.literalExpressionImpl();
        List<IExpression> elements = new ArrayList<>();
        for (ExpressionParser.LiteralExpressionImplContext expression : expressions) {
            IExpression expr = expression.accept(this);
            elements.add(expr);
        }

        return new ComparisonExpression.IN(left, new CollectionExpression(elements));
    }

    @Override
    public IExpression visitBraceExpression(ExpressionParser.BraceExpressionContext ctx) {
        return ctx.getChild(1).accept(this);
    }

    @Override
    public IExpression visitFieldExpression(ExpressionParser.FieldExpressionContext ctx) {
        return new FieldExpression(ctx.getText());
    }

    @Override
    public IExpression visitArrayAccessExpression(ExpressionParser.ArrayAccessExpressionContext ctx) {
        return new ArrayAccessExpression(ctx.subExpression().accept(this), Integer.parseInt(ctx.NUMBER_LITERAL().getText()));
    }

    @Override
    public IExpression visitLiteralExpressionImpl(ExpressionParser.LiteralExpressionImplContext ctx) {
        TerminalNode literalExpressionNode = ctx.getChild(TerminalNode.class, 0);
        switch (literalExpressionNode.getSymbol().getType()) {
            case ExpressionLexer.NUMBER_LITERAL: {
                return new LiteralExpression(Long.parseLong(literalExpressionNode.getText()));
            }
            case ExpressionLexer.STRING_LITERAL: {
                return new LiteralExpression(getUnQuotedString(literalExpressionNode.getSymbol()));
            }
            default:
                throw new RuntimeException("unexpected right expression type");
        }
    }

    @Override
    public IExpression visitFunctionExpression(ExpressionParser.FunctionExpressionContext ctx) {
        String functionName = ctx.functionNameExpression().getText();
        IFunction function = this.functionProvider == null ? null : this.functionProvider.getFunction(functionName);

        List<ExpressionParser.ExpressionContext> parameters = ctx.getRuleContexts(ExpressionParser.ExpressionContext.class);
        int inputParameterSize = parameters.size();
        if (function != null) {
            if (inputParameterSize != function.getParameters().size()) {
                throw new IllegalStateException(StringUtils.format("In expression [%s], function [%s] has [%d] parameters, but only given [%d]",
                                                                   ctx.getText(),
                                                                   function.getName(),
                                                                   function.getParameters().size(),
                                                                   inputParameterSize));
            }
            for (int i = 0; i < inputParameterSize; i++) {
                ExpressionParser.ExpressionContext argExpression = parameters.get(i);
                function.validateParameter(i, argExpression);
            }
        }

        int countOfConstantParameter = 0;
        List<IExpression> parameterExpressionList = new ArrayList<>(parameters.size());
        for (ExpressionParser.ExpressionContext parameter : parameters) {
            IExpression parameterExpression = parameter.accept(this);
            parameterExpressionList.add(parameterExpression);

            if (parameterExpression instanceof LiteralExpression) {
                countOfConstantParameter++;
            }
        }

        IExpression expression = new FunctionExpression(function, functionName, parameterExpressionList);

        // Optimization
        // Calculates the function now and replaces the function expression by the literal expression
        if (countOfConstantParameter == parameterExpressionList.size()) {
            expression = new LiteralExpression(expression.evaluate(null));
        }

        return expression;
    }

    static String getUnQuotedString(Token symbol) {
        CharStream input = symbol.getInputStream();
        if (input == null) {
            return null;
        } else {
            int n = input.size();

            // +1 to skip the leading quoted character
            int s = symbol.getStartIndex() + 1;

            // -1 to skip the ending quoted character
            int e = symbol.getStopIndex() - 1;
            return s < n && e < n ? input.getText(Interval.of(s, e)) : "<EOF>";
        }
    }
}
