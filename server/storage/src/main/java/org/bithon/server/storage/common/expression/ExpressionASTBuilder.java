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
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
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

        // TODO: optimize nested logical expression into one
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
    public IExpression visitNotExpression(ExpressionParser.NotExpressionContext ctx) {
        return new LogicalExpression.NOT(ctx.subExpression().accept(this));
    }

    @Override
    public IExpression visitArithmeticExpression(ExpressionParser.ArithmeticExpressionContext ctx) {
        // There's only one TerminalNode in binaryExpression root definition, use index 0 to get that node
        TerminalNode op = (TerminalNode) ctx.getChild(1);

        switch (op.getSymbol().getType()) {
            case ExpressionLexer.ADD:
                return new ArithmeticExpression.ADD(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.SUB:
                return new ArithmeticExpression.SUB(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.MUL:
                return new ArithmeticExpression.MUL(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.DIV:
                return new ArithmeticExpression.DIV(ctx.getChild(0).accept(this),
                                                    ctx.getChild(2).accept(this));
            default:
                throw new RuntimeException();
        }
    }

    @Override
    public IExpression visitComparisonExpression(ExpressionParser.ComparisonExpressionContext ctx) {
        // There's only one TerminalNode in binaryExpression root definition, use index 0 to get that node
        TerminalNode op = (TerminalNode) ctx.getChild(1);

        ParseTree left = ctx.getChild(0);
        if (!(left instanceof ExpressionParser.IdentifierExpressionContext)) {
            // For simply expression optimization later
            throw new RuntimeException(StringUtils.format("For operator '%s', the left expression must be an identifier.", op));
        }

        switch (op.getSymbol().getType()) {
            case ExpressionLexer.LT:
                return new ComparisonExpression.LT(left.accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.LTE:
                return new ComparisonExpression.LTE(left.accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.GT:
                return new ComparisonExpression.GT(left.accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.GTE:
                return new ComparisonExpression.GTE(left.accept(this),
                                                    ctx.getChild(2).accept(this));

            case ExpressionLexer.NE:
                return new ComparisonExpression.NE(left.accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.EQ:
                return new ComparisonExpression.EQ(left.accept(this),
                                                   ctx.getChild(2).accept(this));

            case ExpressionLexer.LIKE:
                return new ComparisonExpression.LIKE(left.accept(this),
                                                     ctx.getChild(2).accept(this));

            case ExpressionLexer.NOT:
                TerminalNode operator = (TerminalNode) ctx.getChild(2);
                switch (operator.getSymbol().getType()) {
                    case ExpressionLexer.LIKE:
                        return new LogicalExpression.NOT(
                            newLikeExpression(left, ctx.getChild(3))
                        );
                    case ExpressionLexer.IN:
                        return new LogicalExpression.NOT(
                            newInExpression(left, ctx.getChild(3))
                        );
                    default:
                        break;
                }
                throw new RuntimeException();

            case ExpressionLexer.IN:
                return newInExpression(left, ctx.getChild(2));
            default:
                throw new RuntimeException();
        }
    }

    private IExpression newLikeExpression(ParseTree left, ParseTree right) {
        return new ComparisonExpression.LIKE(left.accept(this), right.accept(this));
    }

    private IExpression newInExpression(ParseTree left, ParseTree right) {
        IExpression expression = right.accept(this);
        if ((expression instanceof ExpressionList)) {
            if (((ExpressionList) expression).getExpressions().isEmpty()) {
                throw new RuntimeException("The elements of the IN operator is empty");
            }
            return new ComparisonExpression.IN(left.accept(this), (ExpressionList) expression);
        }
        return new ComparisonExpression.EQ(left.accept(this), expression);
    }

    @Override
    public IExpression visitIdentifierExpression(ExpressionParser.IdentifierExpressionContext ctx) {
        return new IdentifierExpression(ctx.getText());
    }

    @Override
    public IExpression visitArrayAccessExpression(ExpressionParser.ArrayAccessExpressionContext ctx) {
        return new ArrayAccessExpression(ctx.subExpression().accept(this), Integer.parseInt(ctx.NUMBER_LITERAL().getText()));
    }

    @Override
    public IExpression visitLiteralExpression(ExpressionParser.LiteralExpressionContext ctx) {
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
    public IExpression visitExpressionList(ExpressionParser.ExpressionListContext ctx) {
        List<IExpression> expressions = new ArrayList<>();
        for (ParseTree expr : ctx.expressionListImpl().children) {
            if (expr instanceof ExpressionParser.ExpressionContext) {
                expressions.add(expr.accept(this));
            }
        }
        if (expressions.size() == 1) {
            return expressions.get(0);
        }

        return new ExpressionList(expressions);
    }

    @Override
    public IExpression visitFunctionExpression(ExpressionParser.FunctionExpressionContext ctx) {
        String functionName = ctx.getChild(0).getText();

        IFunction function = this.functionProvider == null ? null : this.functionProvider.getFunction(functionName);

        List<ExpressionParser.ExpressionContext> parameters = ctx.expressionListImpl().expression();
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

        IExpression functionExpression = new FunctionExpression(function, functionName, parameterExpressionList);

        // Apply optimization.
        // ALL parameters are literal,
        // calculates the function now and replaces the function expression by the literal expression
        if (countOfConstantParameter == parameterExpressionList.size()) {
            functionExpression = new LiteralExpression(functionExpression.evaluate(null));
        }

        return functionExpression;
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
