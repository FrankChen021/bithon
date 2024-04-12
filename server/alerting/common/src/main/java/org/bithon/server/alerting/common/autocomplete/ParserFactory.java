package org.bithon.server.alerting.common.autocomplete;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

public interface ParserFactory {

    Parser createParser(TokenStream tokenStream);

}
