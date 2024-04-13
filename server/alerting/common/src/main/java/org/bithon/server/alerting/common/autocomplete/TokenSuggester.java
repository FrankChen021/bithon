package org.bithon.server.alerting.common.autocomplete;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Given an ATN state and the lexer ATN, suggests auto-completion texts.
 */
class TokenSuggester implements ISuggester {
    private static final Logger logger = LoggerFactory.getLogger(TokenSuggester.class);

    private final LexerWrapper lexerWrapper;
    private final CasePreference casePreference;

    private final Set<Suggestion> suggestions = new TreeSet<>();
    private final List<Integer> visitedLexerStates = new ArrayList<>();
    private final String origPartialToken;

    private int tokenType;

    public TokenSuggester(LexerWrapper lexerWrapper, String input) {
        this(input, lexerWrapper, CasePreference.UPPER);
    }

    public TokenSuggester(String origPartialToken, LexerWrapper lexerWrapper, CasePreference casePreference) {
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

        this.tokenType = grammarRule.nextTokenType;
        int nextTokenRuleNumber = grammarRule.nextTokenType - 1; // Count from 0 not from 1
        ATNState lexerState = this.lexerWrapper.findStateByRuleNumber(nextTokenRuleNumber);
        suggest("", lexerState, origPartialToken);

        suggestionList.addAll(this.suggestions);

        return true;
    }

    private void suggest(String tokenSoFar, ATNState lexerState, String remainingText) {
        logger.debug("SUGGEST: tokenSoFar={} remainingText={} lexerState={}",
                     tokenSoFar,
                     remainingText,
                     toString(lexerState));

        if (visitedLexerStates.contains(lexerState.stateNumber)) {
            return; // avoid infinite loop and stack overflow
        }
        visitedLexerStates.add(lexerState.stateNumber);
        try {
            Transition[] transitions = lexerState.getTransitions();
            boolean tokenNotEmpty = !tokenSoFar.isEmpty();
            boolean noMoreCharactersInToken = (transitions.length == 0);
            if (tokenNotEmpty && noMoreCharactersInToken) {
                addSuggestedToken(tokenSoFar);
                return;
            }
            for (Transition trans : transitions) {
                suggestViaLexerTransition(tokenSoFar, remainingText, trans);
            }
        } finally {
            visitedLexerStates.remove(visitedLexerStates.size() - 1);
        }
    }

    private String toString(ATNState lexerState) {
        String ruleName = this.lexerWrapper.getRuleNames()[lexerState.ruleIndex];
        return ruleName + " " + lexerState.getClass().getSimpleName() + " " + lexerState;
    }

    private void suggestViaLexerTransition(String tokenSoFar, String remainingText, Transition trans) {
        if (trans.isEpsilon()) {
            suggest(tokenSoFar, trans.target, remainingText);
        } else if (trans instanceof AtomTransition) {
            String newTokenChar = getAddedTextFor((AtomTransition) trans);
            if (remainingText.isEmpty() || remainingText.startsWith(newTokenChar)) {
                logger.debug("LEXER TOKEN: {} remaining={}", newTokenChar, remainingText);
                suggestViaNonEpsilonLexerTransition(tokenSoFar, remainingText, newTokenChar, trans.target);
            } else {
                logger.debug("NON MATCHING LEXER TOKEN: {} remaining={}", newTokenChar, remainingText);
            }
        } else if (trans instanceof SetTransition) {
            List<Integer> symbols = trans.label().toList();
            for (Integer symbol : symbols) {
                char[] charArr = Character.toChars(symbol);
                String charStr = new String(charArr);
                boolean shouldIgnoreCase = shouldIgnoreThisCase(charArr[0], symbols); // TODO: check for non-BMP
                if (!shouldIgnoreCase && (remainingText.isEmpty() || remainingText.startsWith(charStr))) {
                    suggestViaNonEpsilonLexerTransition(tokenSoFar, remainingText, charStr, trans.target);
                }
            }
        }
    }

    private void suggestViaNonEpsilonLexerTransition(String tokenSoFar, String remainingText,
                                                     String newTokenChar, ATNState targetState) {
        String newRemainingText = (!remainingText.isEmpty()) ? remainingText.substring(1) : remainingText;
        suggest(tokenSoFar + newTokenChar, targetState, newRemainingText);
    }

    private void addSuggestedToken(String tokenToAdd) {
        String justTheCompletionPart = chopOffCommonStart(tokenToAdd, this.origPartialToken);
        suggestions.add(new Suggestion(this.tokenType, justTheCompletionPart));
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
