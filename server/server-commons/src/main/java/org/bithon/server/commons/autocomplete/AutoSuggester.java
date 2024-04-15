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
import org.antlr.v4.runtime.atn.RuleStopState;
import org.antlr.v4.runtime.atn.RuleTransition;
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

    private final InputParser parser;
    private final InputLexer lexer;

    // Runtime states
    private List<? extends Token> inputTokens;
    private String untokenizedText = "";
    private String input;
    private final Set<Suggestion> collectedSuggestions = new HashSet<>();

    // Configuration
    private CasePreference casePreference = CasePreference.BOTH;
    private final List<ISuggester> suggesterList = new ArrayList<>();

    public AutoSuggester(LexerAndParserFactory lexerAndParserFactory) {
        this.lexer = new InputLexer(lexerAndParserFactory);
        this.parser = new InputParser(lexerAndParserFactory, lexer.getVocabulary());
    }

    public void setCasePreference(CasePreference casePreference) {
        this.casePreference = casePreference;
    }

    public AutoSuggester addSuggester(ISuggester suggester) {
        suggesterList.add(suggester);
        return this;
    }

    public Collection<Suggestion> suggest(String input) {
        return suggest(input, 0);
    }

    public Collection<Suggestion> suggest(String input, int startingRule) {
        this.input = input;
        this.collectedSuggestions.clear();

        tokenizeInput(input);

        ATNState initialState = this.parser.getATNByRuleNumber(startingRule);
        logger.debug("Parser initial state: {}", initialState);
        parseAndCollectTokenSuggestions(initialState, 0);

        return this.collectedSuggestions;
    }

    private void tokenizeInput(String input) {
        InputLexer.TokenizationResult tokenizationResult = lexer.tokenizeNonDefaultChannel(input);
        this.inputTokens = tokenizationResult.tokens;
        this.untokenizedText = tokenizationResult.untokenizedText;
        if (logger.isDebugEnabled()) {
            logger.debug("TOKENS FOUND IN FIRST PASS: [{}]", this.inputTokens.stream()
                                                                             .map(Token::getText)
                                                                             .collect(Collectors.joining(" ")));
        }
    }

    static class ParseState {
        ATNState atnState;
        int tokenListIndex;
        Set<Integer> visited;
        String indent;
        List<ATNState> followingStates;

        public ParseState(ATNState target, int tokenListIndex) {
            this.atnState = target;
            this.tokenListIndex = tokenListIndex;
            this.indent = "";
            this.visited = new HashSet<>();
            this.visited.add(target.stateNumber);
            this.followingStates = new ArrayList<>();
        }

        public ParseState(ATNState target, int tokenListIndex, ParseState parent, ATNState followingState) {
            this.atnState = target;
            this.tokenListIndex = tokenListIndex;
            this.indent = parent.indent + " ";
            this.visited = new HashSet<>(parent.visited);
            this.visited.add(target.stateNumber);
            this.followingStates = new ArrayList<>(parent.followingStates);
            if (followingState != null) {
                this.followingStates.add(followingState);
            }
        }
    }

    private void parseAndCollectTokenSuggestions(ATNState parserState, int tokenListIndex) {
        Stack<ParseState> stack = new Stack<>();
        stack.push(new ParseState(parserState, tokenListIndex));

        while (!stack.isEmpty()) {
            ParseState state = stack.pop();

            if (logger.isDebugEnabled()) {
                logger.debug("{}State {}, transitions: [{}]",
                             state.indent,
                             parser.toParseStateString(state.atnState),
                             parser.toTransitionListString(state.atnState));
            }

            if (state.tokenListIndex >= inputTokens.size()) {
                suggestNextTokensForParserState(state);
                continue;
            }

            if (state.atnState instanceof RuleStopState) {
                if (!state.followingStates.isEmpty())
                    state.followingStates.remove(state.followingStates.size() - 1);
            }

            Transition[] transitions = state.atnState.getTransitions();
            for (int i = transitions.length - 1; i >= 0; i--) {
                Transition transition = transitions[i];
                if (transition.isEpsilon() && !state.visited.contains(transition.target.stateNumber)) {
                    stack.push(new ParseState(transition.target,
                                              // Epsilon transitions don't consume a token, so don't move the index
                                              state.tokenListIndex,
                                              state,
                                              transition instanceof RuleTransition ? ((RuleTransition) transition).followState : null
                    ));
                } else if (transition instanceof AtomTransition) {
                    Token nextToken = inputTokens.get(state.tokenListIndex);
                    boolean nextTokenMatchesTransition = (((AtomTransition) transition).label == nextToken.getType());

                    logger.debug("{}Token [{}]{} following atomic transition: [{}]",
                                 state.indent,
                                 nextToken.getText(),
                                 nextTokenMatchesTransition ? "" : " NOT",
                                 parser.toTransitionString(transition));
                    if (nextTokenMatchesTransition) {
                        stack.push(new ParseState(transition.target,
                                                  state.tokenListIndex + 1,
                                                  state,
                                                  null));
                    }
                } else if (transition instanceof SetTransition) {
                    Token nextToken = inputTokens.get(state.tokenListIndex);
                    int tokenType = nextToken.getType();
                    if (transition.label().contains(tokenType)) {
                        logger.debug("{}Token [{}] following transition: {} to {}", state.indent, nextToken.getText(), parser.toTransitionString(transition), tokenType);
                        stack.push(new ParseState(transition.target,
                                                  state.tokenListIndex + 1,
                                                  state,
                                                  null));
                    }
                }
            }
        }
    }

    private void suggestNextTokensForParserState(ParseState parseState) {
        Set<GrammarRule> rules = findAllTransitionRules(parseState.atnState, parseState.indent, parseState.followingStates);

        List<ISuggester> suggesters = new ArrayList<>(this.suggesterList);
        suggesters.add(new GrammarSuggester(this.untokenizedText, lexer, this.casePreference));

        Set<Suggestion> suggestions = new HashSet<>();

        // Run suggesters on all rules
        for (GrammarRule rule : rules) {
            for (ISuggester suggester : suggesters) {
                if (!suggester.suggest(this.inputTokens, rule, suggestions)) {
                    // Stop processing further suggesters if the current one says so
                    break;
                }
            }
        }

        validateSuggestions(parseState.atnState, suggestions);
        logger.debug("WILL SUGGEST TOKENS FOR STATE: {}", parser.toParseStateString(parseState.atnState));
    }

    static class FindState {
        ATNState atnState;
        String indent;

        public FindState(ATNState parserState, String indent) {
            this.atnState = parserState;
            this.indent = indent;
        }
    }

    private Set<GrammarRule> findAllTransitionRules(ATNState parserState, String indent, List<ATNState> followingStates) {
        Set<GrammarRule> rules = new HashSet<>();

        Set<TransitionWrapper> visitedTransitions = new HashSet<>();
        Stack<FindState> stack = new Stack<>();
        stack.push(new FindState(parserState, indent));

        int followingStateIndex = followingStates.size() - 1;
        while (!stack.isEmpty()) {
            ATNState followingState = null;

            FindState state = stack.pop();

            if (logger.isDebugEnabled()) {
                logger.debug("{}Finding Rules: State {}, transitions: [{}]",
                             state.indent,
                             parser.toParseStateString(state.atnState),
                             parser.toTransitionListString(state.atnState));
            }

            if (state.atnState instanceof RuleStopState && followingStateIndex >= 0) {
                followingState = followingStates.get(followingStateIndex);
                followingStateIndex--;
            }

            Transition[] transitions = state.atnState.getTransitions();
            for (int i = transitions.length - 1; i >= 0; i--) {
                Transition transition = transitions[i];

                if (followingState != null && !transition.target.equals(followingState))
                    continue;

                TransitionWrapper wrappedTransition = new TransitionWrapper(state.atnState, transition);
                if (visitedTransitions.contains(wrappedTransition)) {
                    logger.debug("{}Not following visited {}", indent, wrappedTransition);
                    continue;
                }

                if (transition.isEpsilon()) {
                    visitedTransitions.add(wrappedTransition);
                    stack.push(new FindState(transition.target, indent + " "));
                } else if (transition instanceof AtomTransition) {
                    int label = ((AtomTransition) transition).label;
                    if (label >= 1) { // EOF would be -1
                        rules.add(new GrammarRule(state.atnState.ruleIndex, label));
                    }
                } else if (transition instanceof SetTransition) {
                    for (Interval interval : transition.label().getIntervals()) {
                        for (int label = interval.a; label <= interval.b; ++label) {
                            rules.add(new GrammarRule(state.atnState.ruleIndex, label));
                        }
                    }
                }
            }
        }

        return rules;
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

        List<? extends Token> completedTextTokens = this.lexer.tokenizeNonDefaultChannel(completedText).tokens;
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
                throw new IllegalStateException("Unexpected: " + parser.toTransitionString(parserTransition));
            }
        }
        return false;
    }
}
