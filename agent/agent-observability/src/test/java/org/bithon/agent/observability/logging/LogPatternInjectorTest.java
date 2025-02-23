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


import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * @author Frank Chen
 * @date 19/3/24 4:59 pm
 */
public class LogPatternInjectorTest {
    @Test
    public void test() {
        Assertions.assertEquals("[bTxId: %X{bTxId}, bSpanId: %X{bSpanId}, bMode: %X{bMode}] %msg", LogPatternInjector.injectTracePattern("%msg"));

        // No injection due to no msg field found
        Assertions.assertEquals("%thread", LogPatternInjector.injectTracePattern("%thread"));

        // No injection due to bTxId variable defined
        Assertions.assertEquals("%X{bTxId}", LogPatternInjector.injectTracePattern("%X{bTxId}"));
    }

    @Test
    public void test_VariableDefined() {
        // No injection due to bTxId variable defined
        Assertions.assertEquals("%X{bTxId}", LogPatternInjector.injectTracePattern("%X{bTxId}"));

        // bTxIdd is not a valid variable, injection will be included
        Assertions.assertEquals("%X{bTxIdd} [bTxId: %X{bTxId}, bSpanId: %X{bSpanId}, bMode: %X{bMode}] %msg",
                            LogPatternInjector.injectTracePattern("%X{bTxIdd} %msg"));

        // bTxId included with default value, no injection involved
        Assertions.assertEquals("%X{bTxId: -null} %msg",
                            LogPatternInjector.injectTracePattern("%X{bTxId: -null} %msg"));

        // No injection due to bTxId defined
        Assertions.assertEquals("%X{test} %X{bTxId: -null} %msg",
                            LogPatternInjector.injectTracePattern("%X{test} %X{bTxId: -null} %msg"));
    }
}
