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

package org.bithon.server.alerting.common.evaluator.rule.builder;

import lombok.Getter;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.bithon.server.alerting.common.evaluator.rule.IAlertRuleEvaluator;
import org.bithon.server.alerting.common.evaluator.rule.UnaryEvaluator;
import org.bithon.server.alerting.common.model.Alert;
import org.bithon.server.alerting.common.model.AlertCondition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/8 5:58 下午
 */
public class RuleEvaluatorBuilder {

    /**
     * @param expression see {@link RuleExpressionLexer}
     */
    public static RuleEvaluatorBuildResult build(Alert alert, Map<String, AlertCondition> conditions, String expression) {
        RuleExpressionLexer lexer = new RuleExpressionLexer(CharStreams.fromString(expression));
        lexer.getErrorListeners().clear();
        lexer.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new InvalidRuleExpressionException(expression, charPositionInLine, msg);
            }
        });
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        RuleExpressionParser parser = new RuleExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer,
                                    Object offendingSymbol,
                                    int line,
                                    int charPositionInLine,
                                    String msg,
                                    RecognitionException e) {
                throw new InvalidRuleExpressionException(expression, charPositionInLine, msg);
            }
        });
        Builder builder = new Builder(alert, expression, conditions);
        return new RuleEvaluatorBuildResult(builder.visit(parser.prog()), builder.getReferencedConditions());
    }

    static class Builder extends RuleExpressionBaseVisitor<IAlertRuleEvaluator> {

        private final String expression;
        private final Map<String, AlertCondition> conditions;

        @Getter
        private final List<String> referencedConditions = new ArrayList<>();

        Builder(Alert alert, String expression, Map<String, AlertCondition> conditions) {
            this.conditions = conditions;
            this.expression = expression;
        }

        @Override
        public IAlertRuleEvaluator visitExpression(RuleExpressionParser.ExpressionContext ctx) {
            if (ctx.getChildCount() == 1) {
                String conditionId = ctx.getChild(0).getText();

                AlertCondition condition = conditions.get(conditionId);
                if (condition == null) {
                    throw new InvalidRuleExpressionException("Condition [%s] in Rule [%s] does not exist", conditionId, expression);
                }
                this.referencedConditions.add(condition.getId());
                return new UnaryEvaluator(condition);
            }

            if (ctx.getChildCount() == 3) {
                String operator = ctx.getChild(1).getText();
                if ("&&".equals(operator)) {
                    IAlertRuleEvaluator leftOp = visit(ctx.getChild(0));
                    IAlertRuleEvaluator rightOp = visit(ctx.getChild(2));
                    return leftOp.and(rightOp);
                }
                if ("||".equals(operator)) {
                    IAlertRuleEvaluator leftOp = visit(ctx.getChild(0));
                    IAlertRuleEvaluator rightOp = visit(ctx.getChild(2));
                    return leftOp.or(rightOp);
                }

                /*
                 * Only one case left here：(A)
                 */
                return visit(ctx.getChild(1));
            }

            /*
             * won't run to here
             */
            return null;
        }
    }
}
