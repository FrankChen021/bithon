package com.sbss.bithon.server.metric.aggregator.spec;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/11
 */
public class InvalidExpressionException extends RuntimeException {
    public InvalidExpressionException(@NotNull String expression, int charPos, String parseExceptionMessage) {
        super(String.format("Invalid expression [%s] at position %d, %s", expression, charPos, parseExceptionMessage));
    }
}
