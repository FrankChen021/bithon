package com.sbss.bithon.collector.datasource.filter;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.collector.datasource.input.InputRow;

import javax.validation.constraints.NotNull;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/24
 */
public class EqualFilter implements IFilter {

    @NotNull
    private final String field;

    @NotNull
    private final Object value;

    public EqualFilter(@JsonProperty("field") String field,
                       @JsonProperty("value") @NotNull Object value) {
        this.field = field;
        this.value = value;
    }

    @Override
    public boolean shouldInclude(InputRow inputRow) {
        return value.equals(inputRow.getColumnValue(this.field));
    }
}
