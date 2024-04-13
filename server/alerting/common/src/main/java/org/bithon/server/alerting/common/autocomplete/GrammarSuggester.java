package org.bithon.server.alerting.common.autocomplete;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

/**
 * Suggests tokens based on ANTLR grammar file.
 */
class GrammarSuggester implements ISuggester {
    private static final Logger logger = LoggerFactory.getLogger(GrammarSuggester.class);

    private final LexerWrapper lexerWrapper;
    private final CasePreference casePreference;
    private final String origPartialToken;

    GrammarSuggester(String origPartialToken,
                     LexerWrapper lexerWrapper,
                     CasePreference casePreference) {
        this.origPartialToken = origPartialToken;
        this.lexerWrapper = lexerWrapper;
        this.casePreference = casePreference;
    }

    /**
     * Suggests auto-completion texts for the next token(s) based on the given parser state from the ATN.
     */
    @Override
    public boolean suggest(List<? extends Token> inputs, GrammarRule grammarRule, List<Suggestion> suggestionList) {
        if (logger.isDebugEnabled()) {
            String ruleNames = lexerWrapper.getRuleNames()[grammarRule.nextTokenType - 1];
            logger.debug("Suggesting tokens for lexer rules: [{}]", ruleNames);
        }

        int nextTokenRuleNumber = grammarRule.nextTokenType - 1; // Count from 0 not from 1
        ATNState lexerState = this.lexerWrapper.findStateByRuleNumber(nextTokenRuleNumber);
        Set<Suggestion> suggestions = suggest(grammarRule.nextTokenType, lexerState);

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

            if (state.parents.contains(lexerState.stateNumber)) {
                continue;
            }
            state.parents.add(lexerState.stateNumber);

            Transition[] transitions = lexerState.getTransitions();
            if (!tokenSoFar.isEmpty() && transitions.length == 0) {
                String justTheCompletionPart = chopOffCommonStart(tokenSoFar, this.origPartialToken);
                suggestions.add(new Suggestion(tokenType, justTheCompletionPart));
                continue;
            }

            for (Transition transition : transitions) {
                if (transition.isEpsilon()) {
                    stack.push(new TraverseState(transition.target, tokenSoFar, remainingText, state.parents));
                    continue;
                }

                if (transition instanceof AtomTransition) {
                    String newTokenChar = getAddedTextFor((AtomTransition) transition);
                    if (remainingText.isEmpty() || remainingText.startsWith(newTokenChar)) {
                        logger.debug("LEXER TOKEN: {} remaining={}", newTokenChar, remainingText);
                        String newRemainingText = (!remainingText.isEmpty()) ? remainingText.substring(1) : remainingText;

                        stack.push(new TraverseState(transition.target, tokenSoFar + newTokenChar, newRemainingText, state.parents));
                    }
                    continue;
                }

                if (transition instanceof SetTransition) {
                    List<Integer> symbols = transition.label().toList();
                    for (Integer symbol : symbols) {
                        char[] charArr = Character.toChars(symbol);
                        String charStr = new String(charArr);
                        boolean shouldIgnoreCase = shouldIgnoreThisCase(charArr[0], symbols); // TODO: check for non-BMP
                        if (!shouldIgnoreCase && (remainingText.isEmpty() || remainingText.startsWith(charStr))) {
                            String newRemainingText = (!remainingText.isEmpty()) ? remainingText.substring(1) : remainingText;

                            stack.push(new TraverseState(transition.target, tokenSoFar + charStr, newRemainingText, state.parents));
                        }
                    }
                }
            }
        }

        return suggestions;
    }

    private String toString(ATNState lexerState) {
        String ruleName = this.lexerWrapper.getRuleNames()[lexerState.ruleIndex];
        return ruleName + " " + lexerState.getClass().getSimpleName() + " " + lexerState;
    }

    private String chopOffCommonStart(String a, String b) {
        int charsToChopOff = Math.min(b.length(), a.length());
        return a.substring(charsToChopOff);
    }

    private String getAddedTextFor(AtomTransition transition) {
        return new String(Character.toChars(transition.label));
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
