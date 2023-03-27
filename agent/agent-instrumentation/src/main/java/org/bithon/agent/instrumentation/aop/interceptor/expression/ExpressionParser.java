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

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionLexer;
import org.bithon.agent.instrumentation.aop.interceptor.InterceptorExpressionParser;
import org.bithon.agent.instrumentation.aop.interceptor.descriptor.InterceptorDescriptor;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/26 19:32
 */
public class ExpressionParser {

    public static InterceptorExpressionParser createGrammarParser(String expression) {
        ErrorListener errorListener = new ErrorListener(expression);
        InterceptorExpressionLexer lexer = new InterceptorExpressionLexer(CharStreams.fromString(expression));
        lexer.getErrorListeners().clear();
        lexer.addErrorListener(errorListener);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        InterceptorExpressionParser parser = new InterceptorExpressionParser(tokens);
        parser.getErrorListeners().clear();
        parser.addErrorListener(errorListener);
        return parser;
    }

    // TODO: NEED to change return
    public static InterceptorDescriptor parse(String expression) {
        InterceptorExpressionParser parser = createGrammarParser(expression);
        parser.parse().accept(new Builder());
        return null;
    }
}
