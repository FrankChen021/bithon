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

package org.bithon.agent.observability.logging;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/7/23 5:25 下午
 */
public class LogPatternInjector {

    public static String injectTracePattern(String userPattern) {
        //
        // Check if the user pattern has already defined the bTxId/bSpanId.
        // If defined, there's no need to inject the trace id automatically.
        //
        Matcher mdcVariable = Pattern.compile("%X\\{([^}]+)}").matcher(userPattern);
        while (mdcVariable.find()) {
            String variable = mdcVariable.group(1);
            variable = variable.split(":")[0].trim();
            if ("bTxId".equals(variable) || "bSpanId".equals(variable)) {
                return userPattern;
            }
        }

        int messageIndex = -1;
        String[] messagePatterns = new String[]{"%m", "%msg", "%message"};
        for (String msgPattern : messagePatterns) {
            int index = userPattern.indexOf(msgPattern);
            if (index != -1) {
                messageIndex = index;
                break;
            }
        }

        /*
         * we assume that a log pattern without the message pattern is not a valid pattern
         */
        if (messageIndex == -1) {
            return userPattern;
        }

        // insert the trace pattern before the message pattern
        StringBuilder newPattern = new StringBuilder(userPattern.substring(0, messageIndex));
        if (newPattern.length() > 0 // messageIndex can be zero if the %msg pattern is the first
            && newPattern.charAt(newPattern.length() - 1) != ' ') {
            newPattern.append(' ');
        }
        newPattern.append("[bTxId: %X{bTxId}, bSpanId: %X{bSpanId}, bMode: %X{bMode}] ");
        newPattern.append(userPattern.substring(messageIndex));

        return newPattern.toString();

    }
}
