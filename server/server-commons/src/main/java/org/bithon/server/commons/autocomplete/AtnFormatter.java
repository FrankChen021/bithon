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

package org.bithon.server.commons.autocomplete;

import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.RuleStartState;
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
import org.antlr.v4.runtime.atn.Transition;

import java.util.Set;
import java.util.TreeSet;

/**
 * Utility class that pretty-prints a textual tree representation of an ATN.
 */
public class AtnFormatter {

    public static String printAtnFor(Lexer lexer) {
        return new LexerAtnPrinter(lexer).atnToString();
    }

    public static String printAtnFor(Parser parser) {
        return new ParserAtnPrinter(parser).atnToString();
    }

    /**
     * Does most of the heavy lifting, leaving how specific transitions are printed to subclasses.
     */
    private abstract static class BaseAtnPrinter<R extends Recognizer<?, ?>> {
        private final StringBuilder result = new StringBuilder();
        private final Set<Integer> visitedStates = new TreeSet<>();
        protected R recognizer;

        private BaseAtnPrinter(R recognizer) {
            this.recognizer = recognizer;
        }

        public String atnToString() {
            appendAtnTree();
            appendTableOfRuleToStartState();
            return result.toString();
        }

        private void appendAtnTree() {
            for (ATNState state : recognizer.getATN().states) {
                if (visitedStates.contains(state.stateNumber)) {
                    continue;
                }
                appendAtnSubtree("", "", state);
            }
        }

        private void appendAtnSubtree(String indent, String transStr, ATNState state) {
            String stateRuleName = (state.ruleIndex >= 0) ? recognizer.getRuleNames()[state.ruleIndex] : "";
            String stateClassName = state.getClass().getSimpleName();
            boolean visitedAlready = visitedStates.contains(state.stateNumber);
            String visitedTag = visitedAlready ? "*" : "";
            String stateStr = stateRuleName + " " + stateClassName + " " + state + visitedTag;
            result.append(indent + transStr + stateStr).append("\n");
            if (visitedAlready) {
                return;
            }
            visitedStates.add(state.stateNumber);
            {
                for (Transition trans : state.getTransitions()) {
                    String newTransStr = trans.getClass().getSimpleName();
                    if (trans instanceof AtomTransition) {
                        newTransStr = toString((AtomTransition) trans);
                    } else if (trans instanceof RuleTransition) {
                        newTransStr += "[FollowState=" + ((RuleTransition) trans).followState.stateNumber + "]";
                    }
                    appendAtnSubtree(indent + "  ", " " + newTransStr + "-> ", trans.target);
                }
            }
        }

        private void appendTableOfRuleToStartState() {
            for (int i = 0; i < recognizer.getATN().ruleToStartState.length; ++i) {
                RuleStartState startState = recognizer.getATN().ruleToStartState[i];
                RuleStopState endState = recognizer.getATN().ruleToStopState[i];
                result.append(String.format("Rule %2d %-20s start: %d  stop: %d", i, recognizer.getRuleNames()[i], startState.stateNumber,
                                            endState.stateNumber));
                result.append("\n");
            }
        }

        abstract protected String toString(AtomTransition trans);

    }

    private static class LexerAtnPrinter extends BaseAtnPrinter<Lexer> {
        private LexerAtnPrinter(Lexer lexer) {
            super(lexer);
        }

        protected String toString(AtomTransition trans) {
            int codePoint = trans.label().get(0);
            return "'" + Character.toChars(codePoint)[0] + "' ";
        }

    }

    private static class ParserAtnPrinter extends BaseAtnPrinter<Parser> {
        private ParserAtnPrinter(Parser parser) {
            super(parser);
        }

        protected String toString(AtomTransition trans) {
            String transDisplayName = recognizer.getVocabulary().getSymbolicName(trans.label);
            return transDisplayName + "(" + trans.label + ") ";
        }

    }

}