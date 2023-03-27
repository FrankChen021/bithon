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

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.antlr.v4.runtime.tree.TerminalNode;

/**
 * @author frank.chen021@outlook.com
 * @date 2023/3/27 23:20
 */
public class ConstExpression {
    private final String text;
    private final int type;

    public ConstExpression(String text, int type) {
        this.text = text;
        this.type = type;
    }

    public ConstExpression(TerminalNode node) {
        this.text = getUnQuotedString(node.getSymbol());
        this.type = node.getSymbol().getType();
    }

    public String getText() {
        return text;
    }

    public int getType() {
        return type;
    }

    private String getUnQuotedString(Token symbol) {
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
