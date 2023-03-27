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

package org.bithon.agent.instrumentation.aop.interceptor.expression;

import org.antlr.v4.runtime.tree.TerminalNode;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionParser;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/27 21:18
 */
public class FunctionCallExpression {
    private final String name;
    private final List<ConstExpression> args;

    public FunctionCallExpression(InterceptorExpressionParser.FunctionCallExpressionContext functionCallExpression) {
        this.name = functionCallExpression.IDENTIFIER().getText();

        InterceptorExpressionParser.FunctionCallArgsExpressionContext argsExpression = functionCallExpression.functionCallArgsExpression();
        if (argsExpression != null) {
            args = argsExpression.constExpression()
                                 .stream()
                                 .map(c -> new ConstExpression((TerminalNode) c.getChild(0)))
                                 .collect(Collectors.toList());
        } else {
            args = Collections.emptyList();
        }
    }

    public String getName() {
        return name;
    }

    public List<ConstExpression> getArgs() {
        return args;
    }
}
