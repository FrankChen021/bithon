package com.sbss.bithon.collector.datasource.typing;

import java.text.DecimalFormat;

/**
 * @author frank.chen021@outlook.com
 * @date
 */
public class LongValueType implements IValueType {

    public final static IValueType INSTANCE = new LongValueType();

    @Override
    public String format(Number value) {
        return new DecimalFormat("#,###").format(value.longValue());
    }

    @Override
    public boolean isGreaterThan(Number left, Number right) {
        return left.longValue() > right.longValue();
    }

    @Override
    public boolean isGreaterThanOrEqual(Number left, Number right) {
        return left.longValue() >= right.longValue();
    }

    @Override
    public boolean isLessThan(Number left, Number right) {
        return left.longValue() < right.longValue();
    }

    @Override
    public boolean isLessThanOrEqual(Number left, Number right) {
        return left.longValue() <= right.longValue();
    }

    @Override
    public boolean isEqual(Number left, Number right) {
        return left.longValue() == right.longValue();
    }

    @Override
    public Number diff(Number left, Number right) {
        return left.longValue() - right.longValue();
    }
}
