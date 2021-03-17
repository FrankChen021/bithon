package com.sbss.bithon.server.metric.aggregator;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.aggregator.ast.PostAggregatorExpressionBaseVisitor;
import com.sbss.bithon.server.metric.aggregator.ast.PostAggregatorExpressionLexer;
import com.sbss.bithon.server.metric.aggregator.ast.PostAggregatorExpressionParser;
import com.sbss.bithon.server.metric.typing.DoubleValueType;
import com.sbss.bithon.server.metric.typing.IValueType;
import com.sbss.bithon.server.metric.typing.LongValueType;
import lombok.Getter;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.tree.TerminalNode;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;

/**
 *
 * @author frankchen
 */
public class PostAggregatorMetricSpec implements IMetricSpec {
    @Getter
    private final String name;

    @Getter
    private final String displayText;

    @Getter
    private final String unit;

    @Getter
    private final String expression;

    @Getter
    private final IValueType valueType;

    @Getter
    private final boolean visible;

    /**
     * runtime property
     */
    @JsonIgnore
    private final ThreadLocal<PostAggregatorExpressionParser> parsers;

    @JsonIgnore
    private DataSourceSchema owner;

    @JsonCreator
    public PostAggregatorMetricSpec(@JsonProperty("name") @NotNull String name,
                                    @JsonProperty("displayText") @NotNull String displayText,
                                    @JsonProperty("unit") @NotNull String unit,
                                    @JsonProperty("expression") @NotNull String expression,
                                    @JsonProperty("valueType") @NotNull String valueType,
                                    @JsonProperty("visible") @Nullable Boolean visible) {
        this.name = name;
        this.displayText = displayText;
        this.unit = unit;
        this.expression = expression.trim();
        this.valueType = "long".equalsIgnoreCase(valueType) ? LongValueType.INSTANCE : DoubleValueType.INSTANCE;
        this.visible = visible == null ? true : visible;

        this.parsers = ThreadLocal.withInitial(()->{
            PostAggregatorExpressionLexer lexer = new PostAggregatorExpressionLexer(CharStreams.fromString(expression));
            lexer.getErrorListeners().clear();
            lexer.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new InvalidExpressionException(expression, charPositionInLine, msg);
                }
            });
            CommonTokenStream tokens = new CommonTokenStream(lexer);
            PostAggregatorExpressionParser parser = new PostAggregatorExpressionParser(tokens);
            parser.getErrorListeners().clear();
            parser.addErrorListener(new BaseErrorListener() {
                @Override
                public void syntaxError(Recognizer<?, ?> recognizer,
                                        Object offendingSymbol,
                                        int line,
                                        int charPositionInLine,
                                        String msg,
                                        RecognitionException e) {
                    throw new InvalidExpressionException(expression, charPositionInLine, msg);
                }
            });
            parser.prog().accept(new PostAggregatorExpressionBaseVisitor<Void>(){
                @Override
                public Void visitTerminal(TerminalNode node) {
                    if ( node.getSymbol().getType() == PostAggregatorExpressionParser.ID ) {
                        if ( !owner.containsMetric(node.getText()) ) {
                            throw new IllegalStateException(String.format("[%s] in [%s] not found in dataSchema", node.getText(), name));
                        }
                    }
                    return null;
                }
            });
            return parser;
        });
    }

    @JsonIgnore
    @Override
    public String getType() {
        return IMetricSpec.POST_AGGREGATOR;
    }

    @Override
    public String validate(Object input) {
        return null;
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return visitor.visit(this);
    }

    @Override
    public void setOwner(DataSourceSchema dataSource) {
        this.owner = dataSource;

        // initialize parser
        this.parsers.get();
    }

    public void visitExpression(PostAggregatorExpressionVisitor visitor) {
        PostAggregatorExpressionParser parser = this.parsers.get();
        parser.reset();
        parser.prog().accept(new PostAggregatorExpressionBaseVisitor<Void>() {
            @Override
            public Void visitExpression(PostAggregatorExpressionParser.ExpressionContext ctx) {
                switch (ctx.getChildCount()) {
                    case 1:
                        return visitChildren(ctx);
                    case 3:
                    {
                        String operator = ctx.getChild(1).getText();
                        switch (operator) {
                            case "+":
                            case "-":
                            case "/":
                            case "*":
                                ctx.getChild(0).accept(this);
                                visitor.visit(operator);
                                ctx.getChild(2).accept(this);
                                return null;
                            default:
                                /*
                                 * 此时只剩一种可能：(A)
                                 */
                                visitor.startBrace();
                                visit(ctx.getChild(1));
                                visitor.endBrace();
                                return null;
                        }
                    }
                    default:
                        // no such case
                        throw new IllegalStateException("ChildCount is " + ctx.getChildCount() + ", Text="+ctx.getText());
                }
            }

            @Override
            public Void visitTerminal(TerminalNode node) {
                switch (node.getSymbol().getType()) {
                    case PostAggregatorExpressionParser.CONST:
                        visitor.visitConst(node.getText());
                        return null;
                    case PostAggregatorExpressionParser.ID:
                        visitor.visitMetric(owner.getMetricSpecByName(node.getText()));
                        return null;
                    default:
                        throw new IllegalStateException("Terminal Node Type:" + node.getSymbol().getType() + ", Input Expression:" + expression);
                }
            }
        });
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PostAggregatorMetricSpec) {
            return this.name.equals(((PostAggregatorMetricSpec) obj).name);
        } else {
            return false;
        }
    }
}
