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
    private final CasePreference casePreference;
    private final Map<Integer, ISuggester> suggesters;
    private Set<Integer>[] ruleVisitedState;

    AutoSuggester(InputParser parser, InputLexer lexer, CasePreference casePreference, Map<Integer, ISuggester> suggesters) {
        this.parser = parser;
        this.lexer = lexer;
        this.casePreference = casePreference;
        this.suggesters = new HashMap<>(suggesters);
    }

    public Collection<Suggestion> suggest(String input) {
        return suggest(input, 0);
    }

    public Collection<Suggestion> suggest(String input, int startingRule) {
        this.input = input;
        this.collectedSuggestions.clear();

        tokenizeInput(input);

        this.ruleVisitedState = new Set[this.inputTokens.size()];
        for (int i = 0; i < this.ruleVisitedState.length; i++) {
            this.ruleVisitedState[i] = new HashSet<>();
        }
        ATNState initialState = this.parser.getATNByRuleNumber(startingRule);
        logger.debug("Parser initial state: {}", initialState);
        parseAndCollectTokenSuggestions(initialState, 0);

        return this.collectedSuggestions;
    }

    public Collection<Suggestion> suggest(ATNGraphGenerator graph, String input, int startingRule) {
        this.input = input;
        ATNState initialState = this.parser.getATNByRuleNumber(startingRule);
        ATNGraphGenerator.IATNNode node = graph.stateToNode.get((long) initialState.stateNumber);
        if (node == null) {
            return new ArrayList<>();
        }
        tokenizeInput(input);

        suggest(graph, initialState, node, new Stack<>(), 0, "");

        return this.collectedSuggestions;
    }

    private void find(ATNGraphGenerator graph,
                      ATNState state,
                      ATNGraphGenerator.IATNNode source) {
        Set<ExpectedToken> tokens = new HashSet<>();

        Stack<ATNGraphGenerator.IATNNode> stack = new Stack<>();
        stack.push(source);

        while (!stack.isEmpty()) {
            ATNGraphGenerator.IATNNode n = stack.pop();
            if (n.isRule()) {
                long id = n.id & 0xFFFFFFFFL;
                ATNGraphGenerator.IATNNode jumpTo = graph.stateToNode.get(id);
                stack.push(jumpTo);
            } else {
                for (ATNGraphGenerator.IATNLink link : n.links) {
                    if (link.transition instanceof AtomTransition) {
                        int label = ((AtomTransition) link.transition).label;
                        if (label >= 1) { // EOF would be -1
                            tokens.add(new ExpectedToken(parser.getRuleName(link.ruleIndex), link.ruleIndex, label));
                        }
                    } else if (link.transition instanceof SetTransition) {
                        for (Interval interval : link.transition.label().getIntervals()) {
                            for (int label = interval.a; label <= interval.b; ++label) {
                                tokens.add(new ExpectedToken(parser.getRuleName(link.ruleIndex), link.ruleIndex, label));
                            }
                        }
                    } else {
                        stack.push(link.target);
                    }
                }
            }
        }

        findSuggestion(state, tokens);
    }

    private int suggest(ATNGraphGenerator graph,
                        ATNState state,
                        ATNGraphGenerator.IATNNode sourceNode,
                        Stack<ATNGraphGenerator.IATNNode> backNodes,
                        int tokenListIndex,
                        String indent) {
        if (tokenListIndex >= inputTokens.size()) {
            find(graph, state, sourceNode);
            return tokenListIndex;
        }

        if (!sourceNode.links.isEmpty() && sourceNode.links.get(0).isTransitsToRule) {
            ATNGraphGenerator.IATNLink link = sourceNode.links.get(0);

            // Jump to rule
            long id = link.target.id & 0xFFFFFFFFL;
            ATNGraphGenerator.IATNNode jumpTo = graph.stateToNode.get(id);

            logger.debug("{}Enter rule: {} -> {}({}), tokenIndex {}", indent, sourceNode.name, jumpTo.name, link.target.id, tokenListIndex);
            tokenListIndex = suggest(graph, link.state, jumpTo, backNodes, tokenListIndex, indent + " ");
            logger.debug("{}Exit rule: {} -> {}({}), tokenIndex {}", indent, sourceNode.name, jumpTo.name, link.target.id, tokenListIndex);

            tokenListIndex = suggest(graph, link.transition.target, link.target, backNodes, tokenListIndex, indent);
            return tokenListIndex;
        }

        for (ATNGraphGenerator.IATNLink link : sourceNode.links) {
            Transition transition = link.transition;
            if (transition instanceof AtomTransition) {
                Token nextToken = inputTokens.get(tokenListIndex);
                boolean nextTokenMatchesTransition = (((AtomTransition) transition).label == nextToken.getType());

                logger.debug("{}Token [{}]{} following atomic transition: [{}]",
                             indent,
                             nextToken.getText(),
                             nextTokenMatchesTransition ? "" : " NOT",
                             parser.toTransitionString(transition));
                if (nextTokenMatchesTransition) {
                    tokenListIndex = suggest(graph, transition.target, link.target, backNodes, tokenListIndex + 1, indent + " ");
                }
            } else if (transition instanceof SetTransition) {
                Token nextToken = inputTokens.get(tokenListIndex);
                int tokenType = nextToken.getType();
                if (transition.label().contains(tokenType)) {
                    logger.debug("{}Token [{}] following transition: {} to {}", indent, nextToken.getText(), parser.toTransitionString(transition), tokenType);
                    tokenListIndex = suggest(graph, transition.target, link.target, backNodes, tokenListIndex + 1, indent + " ");
                }
            } else {
                tokenListIndex = suggest(graph, transition.target, link.target, backNodes, tokenListIndex, indent);
            }
        }

        return tokenListIndex;
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

    public void parse(ATNState state, String input) {
        tokenizeInput(input);
        parse(state, 0, "");
    }

    public int parse(ATNState state, int tokenListIndex, String indent) {
        if (state instanceof RuleStopState) {
            logger.debug("{}End {}", indent.substring(0, indent.length() - 1), parser.toParseStateString(state));
            return tokenListIndex;
        }

        for (Transition transition : state.getTransitions()) {
            if (transition.isEpsilon()) {
                ATNState followState = null;
                if (transition instanceof RuleTransition) {
                    tokenListIndex = parse(transition.target, tokenListIndex, " " + indent);

                    followState = ((RuleTransition) transition).followState;
                }

                // parse sub expression
                logger.debug("{}Enter {} --> {}, index: {}", indent, parser.toParseStateString(state), parser.toParseStateString(transition.target), tokenListIndex);
                tokenListIndex = parse(transition.target, tokenListIndex, " " + indent);
                logger.debug("{}Exit {} --> {}, index{}", indent, parser.toParseStateString(state), parser.toParseStateString(transition.target), tokenListIndex);

                if (followState != null) {
                    tokenListIndex = parse(followState, tokenListIndex, " " + indent);
                }
            } else if (transition instanceof AtomTransition) {
                Token nextToken = inputTokens.get(tokenListIndex);
                boolean nextTokenMatchesTransition = (((AtomTransition) transition).label == nextToken.getType());

                logger.debug("{}Token [{}]{} following atomic transition: [{}]",
                             indent,
                             nextToken.getText(),
                             nextTokenMatchesTransition ? "" : " NOT",
                             parser.toTransitionString(transition));
                if (nextTokenMatchesTransition) {
                    if (tokenListIndex + 1 == inputTokens.size()) {
                        logger.debug("{}FOUND", indent);
                    } else {
                        tokenListIndex = parse(transition.target, tokenListIndex + 1, " " + indent);
                    }
                }
            } else if (transition instanceof SetTransition) {
                Token nextToken = inputTokens.get(tokenListIndex);
                int tokenType = nextToken.getType();
                if (!transition.label().contains(tokenType)) {
                    continue;
                }

                logger.debug("{} Token [{}] following transition: {} to {}", indent, nextToken.getText(), parser.toTransitionString(transition), tokenType);
                if (tokenListIndex + 1 == inputTokens.size()) {
                    logger.debug("{}FOUND", indent);
                } else {
                    tokenListIndex = parse(transition.target, tokenListIndex + 1, " " + indent);
                }
            }
        }

        return tokenListIndex;
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
                if (!state.followingStates.isEmpty()) {
                    state.followingStates.remove(state.followingStates.size() - 1);
                }
            }

            Transition[] transitions = state.atnState.getTransitions();
            for (int i = transitions.length - 1; i >= 0; i--) {
                Transition transition = transitions[i];

                //if(!this.ruleVisitedState[state.tokenListIndex].add(transition.target.stateNumber)) {
                //    continue;
                //}

                if (transition.isEpsilon()
                    && !this.ruleVisitedState[state.tokenListIndex].contains(transition.target.stateNumber)
                    //&& !state.visited.contains(transition.target.stateNumber)
                ) {
                    this.ruleVisitedState[state.tokenListIndex].add(transition.target.stateNumber);

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
        Set<ExpectedToken> tokens = findAllTransitionRules(parseState.atnState,
                                                           parseState.indent,
                                                           parseState.followingStates);

        findSuggestion(parseState.atnState, tokens);
    }

    private void findSuggestion(ATNState atnState, Set<ExpectedToken> tokens) {
        DefaultSuggester defaultSuggester = new DefaultSuggester(this.untokenizedText, lexer, this.casePreference);

        Set<Suggestion> suggestions = new HashSet<>();

        for (ExpectedToken token : tokens) {
            ISuggester suggester = this.suggesters.get(token.parserRuleIndex);
            if (suggester != null) {
                logger.debug("Use rule-based suggester for hint rule index [{}], token = [{}]", token.parserRuleIndex, token.tokenType);
                if (!suggester.suggest(this.inputTokens, token, suggestions)) {
                    continue;
                }
            }

            logger.debug("Use default suggester for hint rule index [{}], token = [{}]", token.parserRuleIndex, token.tokenType);
            defaultSuggester.suggest(this.inputTokens, token, suggestions);
        }

        validateSuggestions(atnState, suggestions);
    }

    static class FindState {
        ATNState atnState;
        String indent;

        public FindState(ATNState parserState, String indent) {
            this.atnState = parserState;
            this.indent = indent;
        }
    }

    private Set<ExpectedToken> findAllTransitionRules(ATNState parserState, String indent, List<ATNState> followingStates) {
        Set<ExpectedToken> tokens = new HashSet<>();

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

                if (followingState != null && !transition.target.equals(followingState)) {
                    continue;
                }

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
                        tokens.add(new ExpectedToken(parser.getRuleName(state.atnState.ruleIndex), state.atnState.ruleIndex, label));
                    }
                } else if (transition instanceof SetTransition) {
                    for (Interval interval : transition.label().getIntervals()) {
                        for (int label = interval.a; label <= interval.b; ++label) {
                            tokens.add(new ExpectedToken(parser.getRuleName(state.atnState.ruleIndex), state.atnState.ruleIndex, label));
                        }
                    }
                }
            }
        }

        return tokens;
    }

    private void validateSuggestions(ATNState parserState, Collection<Suggestion> suggestions) {
        for (Suggestion suggestion : suggestions) {
            if (isValid(parserState, getSuggestionToken(suggestion))) {
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
    private boolean isValid(ATNState atnState, Token newToken) {
        Set<Integer> visited = new HashSet<>();

        Stack<ATNState> stack = new Stack<>();
        stack.push(atnState);

        while (!stack.isEmpty()) {
            atnState = stack.pop();

            Transition[] transitions = atnState.getTransitions();
            for (int i = transitions.length - 1; i >= 0; i--) {
                Transition transition = transitions[i];

                if (transition.isEpsilon()) {
                    if (!visited.add(transition.target.stateNumber)) {
                        continue;
                    }
                    stack.push(transition.target);
                } else if (transition instanceof AtomTransition) {
                    if (((AtomTransition) transition).label == newToken.getType()) {
                        return true;
                    }
                } else if (transition instanceof SetTransition) {
                    if (transition.label().contains(newToken.getType())) {
                        return true;
                    }
                } else {
                    throw new IllegalStateException("Unexpected: " + parser.toTransitionString(transition));
                }
            }
        }

        return false;
    }
}
