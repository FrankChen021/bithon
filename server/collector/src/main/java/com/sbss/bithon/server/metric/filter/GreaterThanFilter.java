package com.sbss.bithon.server.metric.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.server.metric.input.InputRow;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/14
 */
public class GreaterThanFilter implements IFilter {

    @NotNull
    private final String field;

    @NotNull
    private final long threshold;

    public GreaterThanFilter(@JsonProperty("field") String field,
                             @JsonProperty("threshold") @NotNull Long threshold) {
        this.field = field;
        this.threshold = threshold;
    }

    @Override
    public boolean shouldInclude(InputRow inputRow) {
        Object val = inputRow.getCol(this.field);
        if (val instanceof Number) {
            return ((Number) val).longValue() > threshold;
        }
        if (val instanceof String) {
            return (long) Double.parseDouble((String) val) > threshold;
        }
        return false;
    }
}
