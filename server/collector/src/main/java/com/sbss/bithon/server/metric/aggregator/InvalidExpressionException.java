package com.sbss.bithon.server.metric.aggregator;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/11
 */
public class InvalidExpressionException extends RuntimeException {
    public InvalidExpressionException(@NotNull String expression, int charPos, String parseExceptionMessage) {
        super(String.format("表达式[%s]无效: 位置 %d, %s", expression, charPos, parseExceptionMessage));
    }
}
