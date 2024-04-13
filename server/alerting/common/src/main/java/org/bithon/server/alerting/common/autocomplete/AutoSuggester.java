package org.bithon.server.alerting.common.autocomplete;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.SetTransition;
import org.antlr.v4.runtime.atn.Transition;
import org.antlr.v4.runtime.misc.Interval;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

/**
 * Suggests completions for given text, using a given ANTLR4 grammar.
 */
public class AutoSuggester {
    private static final Logger logger = LoggerFactory.getLogger(AutoSuggester.class);

    private final ParserWrapper parserWrapper;
    private final LexerWrapper lexerWrapper;
    private final String input;
    private final Set<Suggestion> collectedSuggestions = new HashSet<>();

    private List<? extends Token> inputTokens;
    private String untokenizedText = "";
    private String indent = "";
    private CasePreference casePreference = CasePreference.BOTH;

    private final Map<ATNState, Integer> parserStateToTokenListIndexWhereLastVisited = new HashMap<>();

    private final List<ISuggester> suggesterList = new ArrayList<>();

    public AutoSuggester(LexerAndParserFactory lexerAndParserFactory, String input) {
        this.lexerWrapper = new LexerWrapper(lexerAndParserFactory);
        this.parserWrapper = new ParserWrapper(lexerAndParserFactory, lexerWrapper.getVocabulary());
        this.input = input;
    }

    public void setCasePreference(CasePreference casePreference) {
        this.casePreference = casePreference;
    }

    public AutoSuggester addSuggester(ISuggester suggester) {
        suggesterList.add(suggester);
        return this;
    }

    public Collection<Suggestion> suggest() {
        tokenizeInput();
        runParserAtnAndCollectSuggestions();
        return collectedSuggestions;
    }

    private void tokenizeInput() {
        LexerWrapper.TokenizationResult tokenizationResult = lexerWrapper.tokenizeNonDefaultChannel(this.input);
        this.inputTokens = tokenizationResult.tokens;
        this.untokenizedText = tokenizationResult.untokenizedText;
        if (logger.isDebugEnabled()) {
            logger.debug("TOKENS FOUND IN FIRST PASS: [{}]", this.inputTokens.stream()
                                                                             .map(Token::getText)
                                                                             .collect(Collectors.joining(" ")));
        }
    }

    private void runParserAtnAndCollectSuggestions() {
        ATNState initialState = this.parserWrapper.getAtnState(0);
        logger.debug("Parser initial state: {}", initialState);
        parseAndCollectTokenSuggestions(initialState, 0);
    }

    /**
     * Recursive through the parser ATN to process all tokens. When successful (out of tokens) - collect completion
     * suggestions.
     */
    private void parseAndCollectTokenSuggestions(ATNState parserState, int tokenListIndex) {
        indent = indent + "  ";
        if (didVisitParserStateOnThisTokenIndex(parserState, tokenListIndex)) {
            logger.debug("{}State {} had already been visited while processing token {}, backtracking to avoid infinite loop.",
                         indent,
                         parserState,
                         tokenListIndex);
            return;
        }
        Integer previousTokenListIndexForThisState = setParserStateLastVisitedOnThisTokenIndex(parserState, tokenListIndex);
        try {
            if (logger.isDebugEnabled()) {
                logger.debug("{}State {}, transitions: [{}]",
                             indent,
                             parserWrapper.toString(parserState),
                             parserWrapper.transitionsStr(parserState));
            }

            if (!haveMoreTokens(tokenListIndex)) { // stop condition for recursion
                suggestNextTokensForParserState(parserState);
                return;
            }
            for (Transition trans : parserState.getTransitions()) {
                if (trans.isEpsilon()) {
                    handleEpsilonTransition(trans, tokenListIndex);
                } else if (trans instanceof AtomTransition) {
                    handleAtomicTransition((AtomTransition) trans, tokenListIndex);
                } else {
                    handleSetTransition((SetTransition) trans, tokenListIndex);
                }
            }
        } finally {
            indent = indent.substring(2);
            setParserStateLastVisitedOnThisTokenIndex(parserState, previousTokenListIndexForThisState);
        }
    }

    private boolean didVisitParserStateOnThisTokenIndex(ATNState parserState, Integer currentTokenListIndex) {
        Integer lastVisitedThisStateAtTokenListIndex = parserStateToTokenListIndexWhereLastVisited.get(parserState);
        return currentTokenListIndex.equals(lastVisitedThisStateAtTokenListIndex);
    }

    private Integer setParserStateLastVisitedOnThisTokenIndex(ATNState parserState, Integer tokenListIndex) {
        if (tokenListIndex == null) {
            return parserStateToTokenListIndexWhereLastVisited.remove(parserState);
        } else {
            return parserStateToTokenListIndexWhereLastVisited.put(parserState, tokenListIndex);
        }
    }

    private boolean haveMoreTokens(int tokenListIndex) {
        return tokenListIndex < inputTokens.size();
    }

    private void handleEpsilonTransition(Transition trans, int tokenListIndex) {
        // Epsilon transitions don't consume a token, so don't move the index
        parseAndCollectTokenSuggestions(trans.target, tokenListIndex);
    }

    private void handleAtomicTransition(AtomTransition trans, int tokenListIndex) {
        Token nextToken = inputTokens.get(tokenListIndex);
        int nextTokenType = inputTokens.get(tokenListIndex).getType();
        boolean nextTokenMatchesTransition = (trans.label == nextTokenType);

        logger.debug("{}Token [{}]{} following atomic transition: [{}]",
                     indent,
                     nextToken.getText(),
                     nextTokenMatchesTransition ? "" : " NOT",
                     parserWrapper.toString(trans));
        if (nextTokenMatchesTransition) {
            parseAndCollectTokenSuggestions(trans.target, tokenListIndex + 1);
        }
    }

    private void handleSetTransition(SetTransition trans, int tokenListIndex) {
        Token nextToken = inputTokens.get(tokenListIndex);
        int nextTokenType = nextToken.getType();
        for (int transitionTokenType : trans.label().toList()) {
            boolean nextTokenMatchesTransition = (transitionTokenType == nextTokenType);
            if (nextTokenMatchesTransition) {
                logger.debug("{}Token {} following transition: {} to {}", indent, nextToken, parserWrapper.toString(trans), transitionTokenType);
                parseAndCollectTokenSuggestions(trans.target, tokenListIndex + 1);
            } else {
                logger.debug("{}Token {} NOT following transition: {} to {}", indent, nextToken, parserWrapper.toString(trans), transitionTokenType);
            }
        }
    }

    private void suggestNextTokensForParserState(ATNState parserState) {
        Set<GrammarRule> rules = new HashSet<>();
        fillParserTransitionLabels(parserState, rules);

        List<ISuggester> suggesters = new ArrayList<>(this.suggesterList);
        // Add default suggester
        suggesters.add(new TokenSuggester(this.untokenizedText, lexerWrapper, this.casePreference));

        List<Suggestion> suggestions = new ArrayList<>();

        for (GrammarRule rule : rules) {
            for (ISuggester suggester : suggesters) {
                if (!suggester.suggest(this.inputTokens, rule, suggestions)) {
                    // Stop processing further suggesters if the current one says so
                    break;
                }
            }
        }

        validateSuggestions(parserState, suggestions);
        logger.debug("WILL SUGGEST TOKENS FOR STATE: {}", parserWrapper.toString(parserState));
    }

    private void fillParserTransitionLabels(ATNState parserState, Collection<GrammarRule> result) {
        Set<TransitionWrapper> visitedTransitions = new HashSet<>();
        Stack<ATNState> stack = new Stack<>();
        stack.push(parserState);

        while (!stack.isEmpty()) {
            ATNState state = stack.pop();

            for (Transition transition : state.getTransitions()) {

                TransitionWrapper wrappedTransition = new TransitionWrapper(state, transition);
                if (visitedTransitions.contains(wrappedTransition)) {
                    logger.debug("{}Not following visited {}", indent, wrappedTransition);
                    continue;
                }

                if (transition.isEpsilon()) {
                    visitedTransitions.add(wrappedTransition);
                    stack.push(transition.target);
                } else if (transition instanceof AtomTransition) {
                    int label = ((AtomTransition) transition).label;
                    if (label >= 1) { // EOF would be -1
                        result.add(new GrammarRule(state.ruleIndex, label));
                    }
                } else if (transition instanceof SetTransition) {
                    for (Interval interval : transition.label().getIntervals()) {
                        for (int i = interval.a; i <= interval.b; ++i) {
                            result.add(new GrammarRule(state.ruleIndex, i));
                        }
                    }
                }
            }
        }
    }

    private void validateSuggestions(ATNState parserState, Collection<Suggestion> suggestions) {
        for (Suggestion suggestion : suggestions) {
            if (isParseable(parserState, getSuggestionToken(suggestion), new HashSet<>())) {
                collectedSuggestions.add(suggestion);
                logger.debug("ADDED suggestion: [{}]", suggestion);
            } else {
                logger.debug("DROPPED non-parseable suggestion: [{}]", suggestion);
            }
        }
    }

    private Token getSuggestionToken(Suggestion suggestion) {
        String completedText = this.input + " " + suggestion.getText();

        List<? extends Token> completedTextTokens = this.lexerWrapper.tokenizeNonDefaultChannel(completedText).tokens;
        if (completedTextTokens.size() <= inputTokens.size()) {
            return null; // Completion didn't yield whole token, could be just a token fragment
        }

        return completedTextTokens.get(completedTextTokens.size() - 1);
    }

    /**
     * Checks if the given token can be parsed by the given parser state.
     */
    private boolean isParseable(ATNState parserState,
                                Token newToken,
                                Set<TransitionWrapper> visitedTransitions) {
        if (newToken == null) {
            return false;
        }
        for (Transition parserTransition : parserState.getTransitions()) {
            if (parserTransition.isEpsilon()) { // Recurse through any epsilon transitionsStr
                TransitionWrapper transWrapper = new TransitionWrapper(parserState, parserTransition);
                if (visitedTransitions.contains(transWrapper)) {
                    continue;
                }
                visitedTransitions.add(transWrapper);
                try {
                    if (isParseable(parserTransition.target, newToken, visitedTransitions)) {
                        return true;
                    }
                } finally {
                    visitedTransitions.remove(transWrapper);
                }
            } else if (parserTransition instanceof AtomTransition) {
                AtomTransition parserAtomTransition = (AtomTransition) parserTransition;
                int transitionTokenType = parserAtomTransition.label;
                if (transitionTokenType == newToken.getType()) {
                    return true;
                }
            } else if (parserTransition instanceof SetTransition) {
                SetTransition parserSetTransition = (SetTransition) parserTransition;
                for (int transitionTokenType : parserSetTransition.label().toList()) {
                    if (transitionTokenType == newToken.getType()) {
                        return true;
                    }
                }
            } else {
                throw new IllegalStateException("Unexpected: " + parserWrapper.toString(parserTransition));
            }
        }
        return false;
    }


}
