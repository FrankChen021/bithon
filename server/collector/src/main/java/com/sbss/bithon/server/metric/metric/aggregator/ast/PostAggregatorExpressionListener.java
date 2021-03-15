// Generated from /Users/frankchen/source/open/bithon/server/collector/src/main/antlr4/PostAggregatorExpression.g4 by ANTLR 4.9.1
package com.sbss.bithon.server.metric.metric.aggregator.ast;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link PostAggregatorExpressionParser}.
 */
public interface PostAggregatorExpressionListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link PostAggregatorExpressionParser#prog}.
	 * @param ctx the parse tree
	 */
	void enterProg(PostAggregatorExpressionParser.ProgContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostAggregatorExpressionParser#prog}.
	 * @param ctx the parse tree
	 */
	void exitProg(PostAggregatorExpressionParser.ProgContext ctx);
	/**
	 * Enter a parse tree produced by {@link PostAggregatorExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void enterExpression(PostAggregatorExpressionParser.ExpressionContext ctx);
	/**
	 * Exit a parse tree produced by {@link PostAggregatorExpressionParser#expression}.
	 * @param ctx the parse tree
	 */
	void exitExpression(PostAggregatorExpressionParser.ExpressionContext ctx);
}