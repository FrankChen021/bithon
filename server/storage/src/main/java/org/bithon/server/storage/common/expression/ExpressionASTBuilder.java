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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.component.commons.expression.ArithmeticExpression;
import org.bithon.component.commons.expression.ArrayAccessExpression;
import org.bithon.component.commons.expression.ComparisonExpression;
import org.bithon.component.commons.expression.ConditionalExpression;
import org.bithon.component.commons.expression.ExpressionList;
import org.bithon.component.commons.expression.FunctionExpression;
import org.bithon.component.commons.expression.IDataType;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.IdentifierExpression;
import org.bithon.component.commons.expression.LiteralExpression;
import org.bithon.component.commons.expression.LogicalExpression;
import org.bithon.component.commons.expression.MacroExpression;
import org.bithon.component.commons.expression.MapAccessExpression;
import org.bithon.component.commons.expression.TernaryExpression;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.function.IFunction;
import org.bithon.component.commons.expression.function.IFunctionProvider;
import org.bithon.component.commons.expression.function.builtin.TimeFunction;
import org.bithon.component.commons.expression.optimzer.ExpressionOptimizer;
import org.bithon.component.commons.expression.validation.ExpressionValidator;
import org.bithon.component.commons.expression.validation.IIdentifier;
import org.bithon.component.commons.expression.validation.IIdentifierProvider;
import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.HumanReadableNumber;
import org.bithon.component.commons.utils.HumanReadablePercentage;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.antlr4.SyntaxErrorListener;
import org.bithon.server.commons.antlr4.TokenUtils;
import org.bithon.server.storage.datasource.ISchema;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author Frank Chen
 * @date 1/7/23 8:37 pm
 */
public class ExpressionASTBuilder {

    public static ExpressionASTBuilder builder() {
        return new ExpressionASTBuilder();
    }

    private IFunctionProvider functions;
    private IIdentifierProvider identifiers;
    private boolean optimizationEnabled = true;

    public ExpressionASTBuilder functions(IFunctionProvider functions) {
        this.functions = functions;
        return this;
    }

    public ExpressionASTBuilder schema(ISchema schema) {
        this.identifiers = schema != null ? new IdentifierProvider(schema) : null;
        return this;
    }

    public ExpressionASTBuilder identifier(IIdentifierProvider identifiers) {
        this.identifiers = identifiers;
        return this;
    }

    public ExpressionASTBuilder optimizationEnabled(boolean enabled) {
        this.optimizationEnabled = enabled;
        return this;
    }

    public IExpression build(String expression) {
        ExpressionLexer lexer = new ExpressionLexer(CharStreams.fromString(expression));
        lexer.getErrorListeners().clear();
        lexer.addErrorListener(SyntaxErrorListener.of(expression));
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        ExpressionParser parser = new ExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(SyntaxErrorListener.of(expression));

        IExpression ast = parser.parse()
                                .expression()
                                .accept(new ParserImpl(functions, this.identifiers));

        ExpressionValidator validator = new ExpressionValidator();
        validator.validate(ast, this.identifiers != null);

        if (this.optimizationEnabled) {
            ast = ExpressionOptimizer.optimize(ast);
        }

        return ast;
    }

    private static class ParserImpl extends ExpressionBaseVisitor<IExpression> {
        private final IFunctionProvider functions;
        private final IIdentifierProvider identifiers;

        private ParserImpl(IFunctionProvider functions,
                           IIdentifierProvider identifiers) {
            this.functions = functions;
            this.identifiers = identifiers;
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
                    // flatten when the parent and sub logical expression have the same operator
                    flattenList.addAll(((LogicalExpression) subexpression).getOperands());
                } else if (logicalOperatorType == ExpressionLexer.OR && LogicalExpression.OR.equals(subExpressionLogicalOperator)) {
                    // flatten when the parent and sub logical expression have the same operator
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

            // NOT logical expression is defined as NotExpression below, not here
            return switch (logicalOperatorType) {
                case ExpressionLexer.AND -> new LogicalExpression.AND(operands);
                case ExpressionLexer.OR -> new LogicalExpression.OR(operands);
                default -> throw new InvalidExpressionException("Unsupported logical operator");
            };
        }

        @Override
        public IExpression visitNotExpressionDecl(ExpressionParser.NotExpressionDeclContext ctx) {
            return new LogicalExpression.NOT(ctx.expression().accept(this));
        }

        @Override
        public IExpression visitArithmeticExpression(ExpressionParser.ArithmeticExpressionContext ctx) {
            // There's only one TerminalNode in binaryExpression root definition, use index 0 to get that node
            TerminalNode op = (TerminalNode) ctx.getChild(1);

            return switch (op.getSymbol().getType()) {
                case ExpressionLexer.ADD ->
                    new ArithmeticExpression.ADD(ctx.getChild(0).accept(this), ctx.getChild(2).accept(this));
                case ExpressionLexer.SUB ->
                    new ArithmeticExpression.SUB(ctx.getChild(0).accept(this), ctx.getChild(2).accept(this));
                case ExpressionLexer.MUL ->
                    new ArithmeticExpression.MUL(ctx.getChild(0).accept(this), ctx.getChild(2).accept(this));
                case ExpressionLexer.DIV ->
                    new ArithmeticExpression.DIV(ctx.getChild(0).accept(this), ctx.getChild(2).accept(this));
                default -> throw new InvalidExpressionException("Unsupported arithmetic operator");
            };
        }

        static class PredicateExpressionVisitor extends ExpressionBaseVisitor<IExpression> {
            private final IExpression left;
            private final IExpression right;

            PredicateExpressionVisitor(IExpression left, IExpression right) {
                this.left = left;
                this.right = right;
            }

            @Override
            public IExpression visitSimplePredicate(ExpressionParser.SimplePredicateContext ctx) {
                TerminalNode op = (TerminalNode) ctx.getChild(0);
                return switch (op.getSymbol().getType()) {
                    case ExpressionLexer.LT -> flipComparisonExpression(new ComparisonExpression.LT(left, right));
                    case ExpressionLexer.LTE -> flipComparisonExpression(new ComparisonExpression.LTE(left, right));
                    case ExpressionLexer.GT -> flipComparisonExpression(new ComparisonExpression.GT(left, right));
                    case ExpressionLexer.GTE -> flipComparisonExpression(new ComparisonExpression.GTE(left, right));
                    case ExpressionLexer.NE -> flipComparisonExpression(new ComparisonExpression.NE(left, right));
                    case ExpressionLexer.EQ -> flipComparisonExpression(new ComparisonExpression.EQ(left, right));
                    default -> throw new InvalidExpressionException("Unsupported type: %d", op.getSymbol().getType());
                };
            }

            @Override
            public IExpression visitExtraPredicate(ExpressionParser.ExtraPredicateContext ctx) {
                TerminalNode op = (TerminalNode) ctx.getChild(0);
                return switch (op.getSymbol().getType()) {
                    case ExpressionLexer.LIKE -> new ConditionalExpression.Like(left, right);
                    case ExpressionLexer.STARTSWITH -> new ConditionalExpression.StartsWith(left, right);
                    case ExpressionLexer.ENDSWITH -> new ConditionalExpression.EndsWith(left, right);
                    case ExpressionLexer.CONTAINS -> new ConditionalExpression.Contains(left, right);
                    default -> throw new InvalidExpressionException("Unsupported type: %d", op.getSymbol().getType());
                };
            }

            @Override
            public IExpression visitNotPredicate(ExpressionParser.NotPredicateContext ctx) {
                IExpression expr = ctx.extraPredicate().accept(this);
                if (expr instanceof ConditionalExpression.Like) {
                    return new ConditionalExpression.NotLike(((ConditionalExpression.Like) expr).getLhs(),
                                                             ((ConditionalExpression.Like) expr).getRhs());
                }
                return new LogicalExpression.NOT(expr);
            }

            // Flip the operands in the comparison expression for simpler optimization/analysis in later steps
            private IExpression flipComparisonExpression(ComparisonExpression comparisonExpression) {
                if (comparisonExpression.getLhs() instanceof LiteralExpression) {
                    if (comparisonExpression.getRhs() instanceof LiteralExpression) {
                        // Constant folding
                        return LiteralExpression.of(comparisonExpression.evaluate(null));
                    }
                    return comparisonExpression.flip();
                }
                return comparisonExpression;
            }
        }

        @Override
        public IExpression visitComparisonExpression(ExpressionParser.ComparisonExpressionContext ctx) {
            IExpression left = ctx.getChild(0).accept(this);
            IExpression right = ctx.getChild(2).accept(this);

            return ctx.getChild(1).accept(new PredicateExpressionVisitor(left, right));
        }

        @Override
        public IExpression visitInExpression(ExpressionParser.InExpressionContext ctx) {
            boolean isNot = ctx.getChild(TerminalNode.class, 0).getSymbol().getType() == ExpressionLexer.NOT;

            IExpression leftOperand = ctx.getChild(0).accept(this);
            IExpression rightOperand;
            if (isNot) {
                rightOperand = ctx.getChild(3).accept(this);
            } else {
                rightOperand = ctx.getChild(2).accept(this);
            }

            // The visitExpressionList will optimize the returning as expression if the list has only one element
            if (rightOperand instanceof ExpressionList expressionList) {
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
                comparisonExpression = new ComparisonExpression.NE(leftOperand, rightOperand);
            } else {
                comparisonExpression = new ComparisonExpression.EQ(leftOperand, rightOperand);
            }
            return flipComparisonExpression(comparisonExpression);
        }

        // Flip the operands in the comparison expression for simpler optimization/analysis in later steps
        private IExpression flipComparisonExpression(ComparisonExpression comparisonExpression) {
            if (comparisonExpression.getLhs() instanceof LiteralExpression) {
                if (comparisonExpression.getRhs() instanceof LiteralExpression) {
                    // Constant folding
                    return LiteralExpression.of(comparisonExpression.evaluate(null));
                }
                return comparisonExpression.flip();
            }
            return comparisonExpression;
        }

        @Override
        public IExpression visitIdentifierExpression(ExpressionParser.IdentifierExpressionContext ctx) {
            String identifier = ctx.getText();
            if (identifiers != null) {
                IIdentifier id = identifiers.getIdentifier(identifier);
                IdentifierExpression expression = new IdentifierExpression(id.getName());
                expression.setDataType(id.getDataType());
                return expression;
            }

            return new IdentifierExpression(ctx.getText());
        }

        @Override
        public IExpression visitArrayAccessExpression(ExpressionParser.ArrayAccessExpressionContext ctx) {
            IExpression expr = ctx.expression().accept(this);
            if (!IDataType.ARRAY.equals(expr.getDataType())) {
                throw new InvalidExpressionException(StringUtils.format("Expression [%s] is not type of array, array access is not allowed.", ctx.expression().getText()));
            }

            return new ArrayAccessExpression(expr, Integer.parseInt(ctx.INTEGER_LITERAL().getText()));
        }

        @Override
        public IExpression visitMapAccessExpression(ExpressionParser.MapAccessExpressionContext ctx) {
            IExpression expr = ctx.expression().accept(this);
            if (expr.getDataType() != null && !IDataType.OBJECT.equals(expr.getDataType())) {
                throw new InvalidExpressionException(StringUtils.format("Expression [%s] is type of [%s], but required OBJECT to perform map access.",
                                                                        ctx.expression().getText(),
                                                                        expr.getDataType().name()));
            }

            return new MapAccessExpression(expr, TokenUtils.getUnQuotedString(ctx.STRING_LITERAL().getSymbol()));
        }

        @Override
        public IExpression visitLiteralExpressionDecl(ExpressionParser.LiteralExpressionDeclContext ctx) {
            if (ctx.children.get(0) instanceof ExpressionParser.DurationLiteralContext durationLiteral) {
                String durationText = durationLiteral.children.get(0).getText();
                LiteralExpression.ReadableDurationLiteral duration = LiteralExpression.of(HumanReadableDuration.parse(durationText));

                // If this expression has a converter,
                // then the literal and its converted will be turned into a FunctionExpression for simplicity
                if (durationLiteral.children.size() > 1) {
                    String converter = durationLiteral.children.get(2).getText();
                    if (converter.startsWith("toMilli")) {
                        return new FunctionExpression(TimeFunction.ToMilliSeconds.INSTANCE, duration);
                    }

                    if (converter.startsWith("toMicro")) {
                        return new FunctionExpression(TimeFunction.ToMicroSeconds.INSTANCE, duration);
                    }

                    if (converter.startsWith("toNano")) {
                        return new FunctionExpression(TimeFunction.ToNanoSeconds.INSTANCE, duration);
                    }

                    // Should never reach here
                    throw new InvalidExpressionException("unexpected converter [%s]", converter);
                }

                return duration;
            }

            TerminalNode literal = ctx.getChild(TerminalNode.class, 0);

            return switch (literal.getSymbol().getType()) {
                case ExpressionLexer.INTEGER_LITERAL ->
                    LiteralExpression.ofLong(Long.parseLong(literal.getText().replace("_", "")));
                case ExpressionLexer.DECIMAL_LITERAL ->
                    LiteralExpression.ofDouble(Double.parseDouble(literal.getText().replace("_", "")));
                case ExpressionLexer.STRING_LITERAL ->
                    LiteralExpression.ofString(TokenUtils.getUnQuotedString(literal.getSymbol()));
                case ExpressionLexer.BOOL_LITERAL ->
                    LiteralExpression.ofBoolean("true".equals(literal.getText().toLowerCase(Locale.ENGLISH)));
                case ExpressionLexer.READABLE_SIZE_LITERAL ->
                    LiteralExpression.of(HumanReadableNumber.of(literal.getText()));
                case ExpressionLexer.READABLE_PERCENTAGE_LITERAL ->
                    LiteralExpression.of(HumanReadablePercentage.parse(literal.getText()));

                default -> throw new InvalidExpressionException("unexpected right expression type");
            };
        }

        @Override
        public IExpression visitExpressionListDecl(ExpressionParser.ExpressionListDeclContext ctx) {
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
        public IExpression visitFunctionExpressionDecl(ExpressionParser.FunctionExpressionDeclContext ctx) {
            IFunction function = null;
            String functionName = ctx.getChild(0).getText();
            if (this.functions != null) {
                function = this.functions.getFunction(functionName);
                if (function == null) {
                    // Only allow defined functions for safe
                    throw new InvalidExpressionException("Function [%s] is not supported.", functionName);
                }
            }

            List<IExpression> argExpressionList = ctx.expressionListDecl()
                                                     .expression()
                                                     .stream()
                                                     .map((exprCtx -> exprCtx.accept(this)))
                                                     .collect(Collectors.toList());

            return new FunctionExpression(function, argExpressionList);
        }

        @Override
        public IExpression visitMacroExpressionDecl(ExpressionParser.MacroExpressionDeclContext ctx) {
            return new MacroExpression(ctx.IDENTIFIER().getText());
        }

        @Override
        public IExpression visitTernaryExpression(ExpressionParser.TernaryExpressionContext ctx) {
            IExpression condition = ctx.expression(0).accept(this);
            IExpression trueExpression = ctx.expression(1).accept(this);
            IExpression falseExpression = ctx.expression(2).accept(this);
            return new TernaryExpression(condition, trueExpression, falseExpression);
        }

        @Override
        public IExpression visitIsNullExpression(ExpressionParser.IsNullExpressionContext ctx) {
            IExpression leftExpression = ctx.expression().accept(this);
            return new ConditionalExpression.IsNull(leftExpression);
        }
    }
}
