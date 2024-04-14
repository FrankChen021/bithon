package org.bithon.server.commons.autocomplete;

import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class DefaultLexerAndParserFactory implements LexerAndParserFactory {

    private final Constructor<? extends Lexer> lexerCtor;
    private final Constructor<? extends Parser> parserCtor;

    public DefaultLexerAndParserFactory(Class<? extends Lexer> lexerClass,
                                        Class<? extends Parser> parserClass) {
        this.lexerCtor = getConstructor(lexerClass, CharStream.class);
        this.parserCtor = getConstructor(parserClass, TokenStream.class);
    }

    @Override
    public Lexer createLexer(CharStream input) {
        return create(lexerCtor, input);
    }

    @Override
    public Parser createParser(TokenStream tokenStream) {
        return create(parserCtor, tokenStream);
    }

    private static <T> Constructor<? extends T> getConstructor(Class<? extends T> givenClass, Class<?> argClass) {
        try {
            return givenClass.getConstructor(argClass);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalArgumentException(
                    givenClass.getSimpleName() + " must have constructor from " + argClass.getSimpleName() + ".");
        }
    }

    private <T> T create(Constructor<? extends T> constructor, Object arg) {
        try {
            return constructor.newInstance(arg);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
