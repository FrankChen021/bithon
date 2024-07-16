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

package org.bithon.server.storage.datasource.input;

import org.antlr.v4.runtime.BaseErrorListener;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.antlr.v4.runtime.RecognitionException;
import org.antlr.v4.runtime.Recognizer;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.Interval;
import org.bithon.server.storage.common.expression.InvalidExpressionException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 16/7/24 2:35 pm
 */
public class PathExpression {
    private final List<String> paths;

    public PathExpression(List<String> paths) {
        this.paths = paths;
    }

    public Object evaluate(IInputRow inputRow) {
        Object obj = inputRow.getCol(paths.get(0));

        for (int i = 1, size = paths.size(); i < size; i++) {
            //noinspection rawtypes
            if (!(obj instanceof Map map)) {
                return obj;
            }
            obj = map.get(paths.get(i));
        }

        return obj;
    }

    public static class Builder {
        public static PathExpression build(String pathExpression) {
            PathExpressionLexer lexer = new PathExpressionLexer(CharStreams.fromString(pathExpression));
            lexer.getErrorListeners().clear();
            lexer.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new InvalidExpressionException(pathExpression, offendingSymbol, line, charPositionInLine, msg);
                }
            });
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PathExpressionParser parser = new PathExpressionParser(tokens);
            parser.getErrorListeners().clear();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new InvalidExpressionException(pathExpression, offendingSymbol, line, charPositionInLine, msg);
                }
            });

            Visitor visitor = new Visitor();
            parser.path().accept(visitor);
            return visitor.toPathExpression();
        }

        private static class Visitor extends PathExpressionBaseVisitor<Void> {
            private final List<String> paths = new ArrayList<>();

            @Override
            public Void visitDotExpression(PathExpressionParser.DotExpressionContext ctx) {
                String path = ctx.IDENTIFIER().getSymbol().getText();
                paths.add(path);

                List<PathExpressionParser.ExpressionContext> subPathExpressions = ctx.expression();
                for (PathExpressionParser.ExpressionContext subPath : subPathExpressions) {
                    subPath.accept(this);
                }

                return null;
            }

            @Override
            public Void visitPropertyAccessExpression(PathExpressionParser.PropertyAccessExpressionContext ctx) {
                paths.add(getUnQuotedString(ctx.STRING_LITERAL().getSymbol()));
                return null;
            }

            static String getUnQuotedString(Token symbol) {
                CharStream input = symbol.getInputStream();
                if (input == null) {
                    return null;
                } else {
                    int n = input.size();

                    // +1 to skip the leading quoted character
                    int s = symbol.getStartIndex() + 1;

                    // -1 to skip the ending quoted character
                    int e = symbol.getStopIndex() - 1;
                    return s < n && e < n ? input.getText(Interval.of(s, e)) : "<EOF>";
                }
            }

            public PathExpression toPathExpression() {
                return new PathExpression(this.paths);
            }
        }
    }
}
