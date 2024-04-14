package org.bithon.server.commons.autocomplete;

import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATN;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.atn.AtomTransition;
import org.antlr.v4.runtime.atn.Transition;

import java.util.Arrays;
import java.util.stream.Collectors;

@Slf4j
class InputParser {
    private final Vocabulary lexerVocabulary;

    private final ATN parserATN;
    private final String[] parserRuleNames;

    public InputParser(ParserFactory parserFactory, Vocabulary lexerVocabulary) {
        this.lexerVocabulary = lexerVocabulary;

        Parser parser = parserFactory.createParser(null);
        this.parserATN = parser.getATN();
        this.parserRuleNames = parser.getRuleNames();

        if (log.isDebugEnabled()) {
            log.debug("Parser rule names: [{}]", String.join(", ", parser.getRuleNames()));
        }
    }

    public ATNState getATNState(int stateNumber) {
        return parserATN.states.get(stateNumber);
    }

    // Bellow are functions for debugging purposes
    public String toParseStateString(ATNState state) {
        String ruleName = this.parserRuleNames[state.ruleIndex];
        return "*" + ruleName + "* " + state.getClass().getSimpleName() + " " + state;
    }

    public String toTransitionString(Transition t) {
        String nameOrLabel = t.getClass().getSimpleName();
        if (t instanceof AtomTransition) {
            nameOrLabel += ' ' + this.lexerVocabulary.getDisplayName(((AtomTransition) t).label);
        }
        return nameOrLabel + " -> " + toParseStateString(t.target);
    }

    public String toTransitionListString(ATNState state) {
        return Arrays.stream(state.getTransitions())
                     .map(this::toTransitionString)
                     .collect(Collectors.joining(", "));
    }
}
