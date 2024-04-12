package org.bithon.server.alerting.common.autocomplete;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;

public interface LexerFactory {

    Lexer createLexer(CharStream input);
}
