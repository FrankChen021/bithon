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

package org.bithon.server.alerting.common.autocomplete;

import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.DecisionState;
import org.antlr.v4.runtime.atn.Transition;
import org.bithon.server.alerting.common.parser.AlertExpressionLexer;
import org.bithon.server.alerting.common.parser.AlertExpressionParser;
import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 12/4/24 2:17 pm
 */
public class AutoCompleteTest {

    @Test
    public void test() {
        String input;
        ReflectionLexerAndParserFactory factory = new ReflectionLexerAndParserFactory(
            AlertExpressionLexer.class,
            AlertExpressionParser.class
        );
        AutoSuggester suggester = new AutoSuggester(factory, "sum BY ");
        suggester.setCasePreference(CasePreference.UPPER);
        System.out.println(suggester.suggestCompletions());
    }


    public static void main(String[] args) {
        // Replace YourParserName with the name of your generated parser class
        AlertExpressionParser parser = new AlertExpressionParser(null);
        ATN atn = parser.getATN();

        // Dump the ATN
        dumpATN(parser, atn);
    }

    private static void dumpATN(AlertExpressionParser parser, ATN atn) {
        System.out.println("ATN:");
        System.out.println("States:");

        // Iterate over all ATN states
        for (int i = 0; i < atn.states.size(); i++) {
            ATNState state = atn.states.get(i);
            System.out.println("  State " + i + ": " + state + " " + state.getClass().getSimpleName());

            // If the state is a decision state, dump its transitions
            if (state instanceof DecisionState) {
                DecisionState decisionState = (DecisionState) state;
                System.out.println("    Decision: " + decisionState.decision);
                System.out.println("    Transitions:");

                // Dump transitions for the decision state
//                for (Transition transition : decisionState.getTransitions()) {
//                    System.out.println("      " + transition);
//                }
            }
            for (Transition transition : state.getTransitions()) {
                System.out.println("  Transition: " + transition.getClass().getSimpleName() + ", to state " + transition.target
                                   + ", rule = " + parser.getRuleNames()[transition.target.ruleIndex]);
            }
        }
    }
}


