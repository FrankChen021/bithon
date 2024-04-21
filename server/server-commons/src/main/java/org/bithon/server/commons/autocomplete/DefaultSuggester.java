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

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

/**
 * Suggests tokens based on ANTLR grammar file.
 */
class DefaultSuggester implements ISuggester {
    private static final Logger logger = LoggerFactory.getLogger(DefaultSuggester.class);

    private final InputLexer inputLexer;
    private final CasePreference casePreference;
    private final String origPartialToken;

    DefaultSuggester(String origPartialToken,
                     InputLexer inputLexer,
                     CasePreference casePreference) {
        this.origPartialToken = origPartialToken;
        this.inputLexer = inputLexer;
        this.casePreference = casePreference;
    }

    /**
     * Suggests auto-completion texts for the next token(s) based on the given parser state from the ATN.
     */
    @Override
    public boolean suggest(List<? extends Token> inputs, ExpectedToken token, Collection<Suggestion> suggestionList) {
        if (logger.isDebugEnabled()) {
            String ruleNames = inputLexer.getRuleNames()[token.tokenType - 1];
            logger.debug("Suggesting tokens for lexer rules: [{}]", ruleNames);
        }

        int nextTokenRuleNumber = token.tokenType - 1; // Count from 0 not from 1
        ATNState lexerState = this.inputLexer.findStateByRuleNumber(nextTokenRuleNumber);
        Set<Suggestion> suggestions = suggest(token.tokenType, lexerState);

        suggestionList.addAll(suggestions);

        return true;
    }

    static class TraverseState {
        ATNState lexerState;
        String tokenSoFar;
        String remainingText;
        Set<Integer> parents;

        public TraverseState(ATNState lexerState, String tokenSoFar, String remainingText, Set<Integer> parents) {
            this.lexerState = lexerState;
            this.tokenSoFar = tokenSoFar;
            this.remainingText = remainingText;
            this.parents = new HashSet<>(parents);
        }
    }

    private Set<Suggestion> suggest(int tokenType, ATNState initState) {
        Set<Suggestion> suggestions = new TreeSet<>();

        Stack<TraverseState> stack = new Stack<>();
        stack.push(new TraverseState(initState, "", this.origPartialToken, new HashSet<>()));

        while (!stack.isEmpty()) {
            TraverseState state = stack.pop();
            ATNState lexerState = state.lexerState;
            String tokenSoFar = state.tokenSoFar;
            String remainingText = state.remainingText;

            if (!state.parents.add(lexerState.stateNumber)) {
                continue;
            }

            Transition[] transitions = lexerState.getTransitions();
            if (!tokenSoFar.isEmpty() && transitions.length == 0) {
                String justTheCompletionPart = chopOffCommonStart(tokenSoFar, this.origPartialToken);
                suggestions.add(Suggestion.of(tokenType, justTheCompletionPart));
                continue;
            }

            for (Transition transition : transitions) {
                if (transition.isEpsilon()) {
                    stack.push(new TraverseState(transition.target,
                                                 tokenSoFar,
                                                 remainingText,
                                                 state.parents));
                    continue;
                }

                if (transition instanceof AtomTransition) {
                    String newToken = new String(Character.toChars(((AtomTransition) transition).label));

                    if (remainingText.isEmpty() || remainingText.startsWith(newToken)) {
                        logger.debug("LEXER TOKEN: {} remaining={}", newToken, remainingText);
                        String newRemainingText = (!remainingText.isEmpty()) ? remainingText.substring(1) : remainingText;

                        stack.push(new TraverseState(transition.target,
                                                     tokenSoFar + newToken,
                                                     newRemainingText,
                                                     state.parents));
                    }
                    continue;
                }

                if (transition instanceof SetTransition) {
                    List<Integer> symbols = transition.label().toList();
                    for (Integer symbol : symbols) {
                        char[] charArr = Character.toChars(symbol);
                        String newToken = new String(charArr);
                        boolean shouldIgnoreCase = shouldIgnoreThisCase(charArr[0], symbols); // TODO: check for non-BMP
                        if (!shouldIgnoreCase && (remainingText.isEmpty() || remainingText.startsWith(newToken))) {
                            String newRemainingText = (!remainingText.isEmpty()) ? remainingText.substring(1) : remainingText;

                            stack.push(new TraverseState(transition.target,
                                                         tokenSoFar + newToken,
                                                         newRemainingText,
                                                         state.parents));
                        }
                    }
                }
            }
        }

        return suggestions;
    }

    private String toString(ATNState lexerState) {
        String ruleName = this.inputLexer.getRuleNames()[lexerState.ruleIndex];
        return ruleName + " " + lexerState.getClass().getSimpleName() + " " + lexerState;
    }

    private String chopOffCommonStart(String a, String b) {
        int charsToChopOff = Math.min(b.length(), a.length());
        return a.substring(charsToChopOff);
    }

    private boolean shouldIgnoreThisCase(char transChar, List<Integer> allTransChars) {
        if (this.casePreference == null) {
            return false;
        }
        switch (this.casePreference) {
            case LOWER:
                return Character.isUpperCase(transChar) && allTransChars.contains((int) Character.toLowerCase(transChar));
            case UPPER:
                return Character.isLowerCase(transChar) && allTransChars.contains((int) Character.toUpperCase(transChar));
            default:
                return false;
        }
    }
}
