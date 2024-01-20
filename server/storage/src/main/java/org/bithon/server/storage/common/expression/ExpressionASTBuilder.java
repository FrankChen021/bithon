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
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.server.datasource.ast.ExpressionBaseVisitor;
import org.bithon.server.datasource.ast.ExpressionLexer;
import org.bithon.server.datasource.ast.ExpressionParser;
import org.bithon.server.storage.datasource.builtin.Functions;
import org.bithon.server.storage.datasource.builtin.IFunctionProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * @author Frank Chen
 * @date 1/7/23 8:37 pm
 */
public class ExpressionASTBuilder extends ExpressionBaseVisitor<IExpression> {

    public static IExpression build(String expression) {
        return build(expression, Functions.getInstance());
    }

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

        IExpression ast = parser.parse()
                                .expression()
                                .accept(new ExpressionASTBuilder(functionProvider));
        return ExpressionOptimizer.optimize(ast);
    }

    private final IFunctionProvider functionProvider;

    private ExpressionASTBuilder(IFunctionProvider functionProvider) {
        this.functionProvider = functionProvider;
    }

    /**
     * <pre>
     * The grammar defined in ANTLR4 for logical expression is as left recursive, that's to say for the expression:
     * A > B AND B > C AND C > D
     * The parsed structure in ANTLR4 is as:
     *
     *           AND
     *          /   \
     *        AND   C > D
     *      /   \
     *  A > B    B > C
     *
     * For such case, we can flatten this AST tree to make it much simpler as:
     *
     *            AND
     *        /    |   \
     *      /      |    \
     *    /        |     \
     *  A > B    B > C   C > D
     * </pre>
     */
    private void flattenLogicalExpression(List<IExpression> flattenList, int logicalOperatorType, IExpression subexpression) {
        if (subexpression instanceof LogicalExpression) {
            String subExpressionLogicalOperator = ((LogicalExpression) subexpression).getOperator();
            if (logicalOperatorType == ExpressionLexer.AND && LogicalExpression.AND.equals(subExpressionLogicalOperator)) {
                // flatten when the parent and sub logical expression have same operator
                flattenList.addAll(((LogicalExpression) subexpression).getOperands());
            } else if (logicalOperatorType == ExpressionLexer.OR && LogicalExpression.OR.equals(subExpressionLogicalOperator)) {
                // flatten when the parent and sub logical expression have same operator
                flattenList.addAll(((LogicalExpression) subexpression).getOperands());
            } else {
                flattenList.add(subexpression);
            }
        } else {
            flattenList.add(subexpression);
        }
    }

    /**
     * a AND b AND c NULL
     *
     * @param ctx the parse tree
     */
    @Override
    public IExpression visitLogicalExpression(ExpressionParser.LogicalExpressionContext ctx) {
        TerminalNode op = (TerminalNode) ctx.getChild(1);
        int logicalOperatorType = op.getSymbol().getType();

        List<IExpression> operands = new ArrayList<>();

        // Apply optimization rules to fold sub expressions together
        flattenLogicalExpression(operands, logicalOperatorType, ctx.getChild(0).accept(this));
        flattenLogicalExpression(operands, logicalOperatorType, ctx.getChild(2).accept(this));

        switch (logicalOperatorType) {
            case ExpressionLexer.AND:
                return new LogicalExpression.AND(operands);

            case ExpressionLexer.OR:
                return new LogicalExpression.OR(operands);

            default:
                // NOT logical expression is defined as NotExpression below, not here
                throw new InvalidExpressionException("Unsupported logical operator");
        }
    }

    @Override
    public IExpression visitNotExpression(ExpressionParser.NotExpressionContext ctx) {
        return new LogicalExpression.NOT(ctx.expression().accept(this));
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
                throw new InvalidExpressionException("Unsupported arithmetic operator");
        }
    }

    @Override
    public IExpression visitComparisonExpression(ExpressionParser.ComparisonExpressionContext ctx) {
        // There's only one TerminalNode in binaryExpression root definition, use index 0 to get that node
        TerminalNode op = (TerminalNode) ctx.getChild(1);

        ParseTree left = ctx.getChild(0);

        switch (op.getSymbol().getType()) {
            case ExpressionLexer.LT:
                return flipComparisonExpression(new ComparisonExpression.LT(left.accept(this),
                                                                            ctx.getChild(2).accept(this)));

            case ExpressionLexer.LTE:
                return flipComparisonExpression(new ComparisonExpression.LTE(left.accept(this),
                                                                             ctx.getChild(2).accept(this)));

            case ExpressionLexer.GT:
                return flipComparisonExpression(new ComparisonExpression.GT(left.accept(this),
                                                                            ctx.getChild(2).accept(this)));

            case ExpressionLexer.GTE:
                return flipComparisonExpression(new ComparisonExpression.GTE(left.accept(this),
                                                                             ctx.getChild(2).accept(this)));

            case ExpressionLexer.NE:
                return flipComparisonExpression(new ComparisonExpression.NE(left.accept(this),
                                                                            ctx.getChild(2).accept(this)));

            case ExpressionLexer.EQ:
                return flipComparisonExpression(new ComparisonExpression.EQ(left.accept(this),
                                                                            ctx.getChild(2).accept(this)));

            case ExpressionLexer.LIKE:
                return createLikeExpression(left, ctx.getChild(2), false);

            case ExpressionLexer.NOT:
                TerminalNode operator = (TerminalNode) ctx.getChild(2);
                switch (operator.getSymbol().getType()) {
                    case ExpressionLexer.LIKE:
                        return createLikeExpression(left, ctx.getChild(3), true);

                    case ExpressionLexer.IN:
                        return createInExpression(left, ctx.getChild(3), true);

                    default:
                        throw new InvalidExpressionException("Unsupported symbol after the NOT operator.");
                }

            case ExpressionLexer.IN:
                return createInExpression(left, ctx.getChild(2), false);

            default:
                throw new InvalidExpressionException("Unsupported type: %d", op.getSymbol().getType());
        }
    }

    // Flip the operands in the comparison expression for simpler optimization/analysis in later steps
    private IExpression flipComparisonExpression(ComparisonExpression comparisonExpression) {
        if (comparisonExpression.getLeft() instanceof LiteralExpression) {
            if (comparisonExpression.getRight() instanceof LiteralExpression) {
                // Constant folding
                return LiteralExpression.create(comparisonExpression.evaluate(null));
            }
            return comparisonExpression.flip();
        }
        return comparisonExpression;
    }

    private IExpression createLikeExpression(ParseTree left, ParseTree right, boolean isNot) {
        IExpression leftOperand = left.accept(this);
        IExpression rightOperand = right.accept(this);

        return isNot ? new ConditionalExpression.NotLike(leftOperand, rightOperand)
            : new ConditionalExpression.Like(leftOperand, rightOperand);
    }

    private IExpression createInExpression(ParseTree left, ParseTree right, boolean isNot) {
        IExpression leftOperand = left.accept(this);

        IExpression rightOperand = right.accept(this);
        if (rightOperand instanceof ExpressionList) {
            ExpressionList expressionList = (ExpressionList) rightOperand;

            if (expressionList.getExpressions().isEmpty()) {
                throw new RuntimeException("The elements of the IN operator is empty");
            }

            if (isNot) {
                return new ConditionalExpression.NotIn(leftOperand, (ExpressionList) rightOperand);
            } else {
                return new ConditionalExpression.In(leftOperand, (ExpressionList) rightOperand);
            }
        }

        // The ExpressionList actually has only subexpression,
        // It will be turned into EQ/NE
        ComparisonExpression comparisonExpression;
        if (isNot) {
            comparisonExpression = new ComparisonExpression.NE(leftOperand,
                                                               rightOperand);

        } else {
            comparisonExpression = new ComparisonExpression.EQ(leftOperand,
                                                               rightOperand);
        }
        return flipComparisonExpression(comparisonExpression);
    }

    @Override
    public IExpression visitIdentifierExpression(ExpressionParser.IdentifierExpressionContext ctx) {
        return new IdentifierExpression(ctx.getText());
    }

    @Override
    public IExpression visitArrayAccessExpression(ExpressionParser.ArrayAccessExpressionContext ctx) {
        return new ArrayAccessExpression(ctx.expression().accept(this), Integer.parseInt(ctx.INTEGER_LITERAL().getText()));
    }

    @Override
    public IExpression visitMapAccessExpression(ExpressionParser.MapAccessExpressionContext ctx) {
        return new MapAccessExpression(ctx.expression().accept(this), getUnQuotedString(ctx.STRING_LITERAL().getSymbol()));
    }

    @Override
    public IExpression visitLiteralExpression(ExpressionParser.LiteralExpressionContext ctx) {
        TerminalNode literalExpressionNode = ctx.getChild(TerminalNode.class, 0);
        switch (literalExpressionNode.getSymbol().getType()) {
            case ExpressionLexer.INTEGER_LITERAL: {
                return LiteralExpression.create(Long.parseLong(literalExpressionNode.getText()));
            }
            case ExpressionLexer.DECIMAL_LITERAL: {
                return LiteralExpression.create(Double.parseDouble(literalExpressionNode.getText()));
            }
            case ExpressionLexer.STRING_LITERAL: {
                return LiteralExpression.create(getUnQuotedString(literalExpressionNode.getSymbol()));
            }
            case ExpressionLexer.BOOL_LITERAL: {
                return LiteralExpression.create("true".equals(literalExpressionNode.getText().toLowerCase(Locale.ENGLISH)));
            }
            default:
                throw new InvalidExpressionException("unexpected right expression type");
        }
    }

    @Override
    public IExpression visitExpressionList(ExpressionParser.ExpressionListContext ctx) {
        List<IExpression> expressions = new ArrayList<>();
        for (ParseTree expr : ctx.children) {
            if (expr instanceof ExpressionParser.ExpressionContext) {
                expressions.add(expr.accept(this));
            }
        }

        if (expressions.size() == 1) {
            // This can be seen as removing of the parentheses
            return expressions.get(0);
        }

        return new ExpressionList(expressions);
    }

    @Override
    public IExpression visitFunctionExpression(ExpressionParser.FunctionExpressionContext ctx) {
        List<ExpressionParser.ExpressionContext> parameters = ctx.expressionList().expression();
        List<IExpression> parameterExpressionList = new ArrayList<>(parameters.size());
        for (ExpressionParser.ExpressionContext parameter : parameters) {
            IExpression parameterExpression = parameter.accept(this);
            parameterExpressionList.add(parameterExpression);
        }

        IFunction function = null;
        String functionName = ctx.getChild(0).getText();
        if (this.functionProvider != null) {
            function = this.functionProvider.getFunction(functionName);
            if (function == null) {
                // Only allow defined functions for safe
                throw new InvalidExpressionException("Function [%s] is not supported.", functionName);
            }
            function.validateParameter(parameterExpressionList);
        }

        return new FunctionExpression(function, parameterExpressionList);
    }

    @Override
    public IExpression visitMacroExpression(ExpressionParser.MacroExpressionContext ctx) {
        return new MacroExpression(ctx.IDENTIFIER().getText());
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
