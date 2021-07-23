/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.plugin.logback.interceptor;

import com.sbss.bithon.agent.bootstrap.aop.AbstractInterceptor;
import com.sbss.bithon.agent.bootstrap.aop.AopContext;
import com.sbss.bithon.agent.bootstrap.aop.InterceptionDecision;

/**
 * {@link ch.qos.logback.core.pattern.PatternLayoutBase#setPattern(String)}
 * <p>
 * add txId:spanId pattern to the user's pattern
 * <p>
 * [bTxId:xxx, bSpanId:xxx]
 *
 * @author frank.chen021@outlook.com
 * @date 2021/7/23 3:25 下午
 */
public class PatternLayoutSetPattern extends AbstractInterceptor {

    @Override
    public InterceptionDecision onMethodEnter(AopContext aopContext) throws Exception {
        String userPattern = aopContext.getArgAs(0);

        if (!userPattern.contains("%X{bTxId}") && !userPattern.contains("%X{bSpanId}")) {

            int messageIndex = -1;
            String[] messagePatterns = new String[]{"%m", "%msg", "%message"};
            for (String msgPattern : messagePatterns) {
                int index = userPattern.indexOf(msgPattern);
                if (index != -1) {
                    messageIndex = index;
                    break;
                }
            }

            // insert the trace pattern before the message pattern
            if (messageIndex != -1) {
                StringBuilder newPattern = new StringBuilder(userPattern.substring(0, messageIndex));
                if (newPattern.charAt(newPattern.length() - 1) != ' ') {
                    newPattern.append(' ');
                }
                newPattern.append("[bTxId:%X{bTxId}, bSpanId:%X{bSpanId}] ");
                newPattern.append(userPattern.substring(messageIndex));

                aopContext.getArgs()[0] = newPattern.toString();
            } else {
                aopContext.getArgs()[0] = userPattern + "[bTxId:%X{bTxId}, bSpan:%X{bSpanId}]";
            }
        }

        return InterceptionDecision.SKIP_LEAVE;
    }
}
