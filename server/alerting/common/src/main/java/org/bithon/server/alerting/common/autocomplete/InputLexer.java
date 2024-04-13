package org.bithon.server.alerting.common.autocomplete;

import org.antlr.v4.runtime.ANTLRErrorListener;
import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.Vocabulary;
import org.antlr.v4.runtime.atn.ATNState;
import org.antlr.v4.runtime.misc.ParseCancellationException;

import java.util.List;
import java.util.stream.Collectors;

class InputLexer {
    private final LexerFactory lexerFactory;
    private Lexer cachedLexer;

    static class TokenizationResult {
        public List<? extends Token> tokens;
        public String untokenizedText = "";
    }

    public InputLexer(LexerFactory lexerFactory) {
        this.lexerFactory = lexerFactory;
    }

    public TokenizationResult tokenizeNonDefaultChannel(String input) {
        TokenizationResult result = this.tokenize(input);
        result.tokens = result.tokens.stream()
                                     .filter(t -> t.getChannel() == Token.DEFAULT_CHANNEL)
                                     .collect(Collectors.toList());
        return result;
    }

    public String[] getRuleNames() {
        return getCachedLexer().getRuleNames();
    }

    public ATNState findStateByRuleNumber(int ruleNumber) {
        return getCachedLexer().getATN().ruleToStartState[ruleNumber];
    }

    public Vocabulary getVocabulary() {
        return getCachedLexer().getVocabulary();
    }

    private Lexer getCachedLexer() {
        if (cachedLexer == null) {
            cachedLexer = createLexer("");
        }
        return cachedLexer;
    }

    private TokenizationResult tokenize(String input) {
        Lexer lexer = this.createLexer(input);
        lexer.removeErrorListeners();
        final TokenizationResult result = new TokenizationResult();
        ANTLRErrorListener newErrorListener = new BaseErrorListener() {
            @Override
            public void syntaxError(Recognizer<?, ?> recognizer, Object offendingSymbol, int line,
                                    int charPositionInLine, String msg, RecognitionException e) throws ParseCancellationException {
                result.untokenizedText = input.substring(charPositionInLine); // intended side effect
            }
        };
        lexer.addErrorListener(newErrorListener);
        result.tokens = lexer.getAllTokens();
        return result;
    }

    private Lexer createLexer(String lexerInput) {
        return this.lexerFactory.createLexer(CharStreams.fromString(lexerInput));
    }
}
