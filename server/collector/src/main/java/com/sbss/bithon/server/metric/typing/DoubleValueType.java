package com.sbss.bithon.server.metric.typing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;

/**
 * @author
 * @date
 */
public class DoubleValueType implements IValueType {

    public static final IValueType INSTANCE = new DoubleValueType();

    @Override
    public String format(Number value) {
        return new DecimalFormat("#,###.00").format(value.doubleValue());
    }

    @Override
    public boolean isGreaterThan(Number left, Number right) {
        return left.doubleValue() > right.doubleValue();
    }

    @Override
    public boolean isGreaterThanOrEqual(Number left, Number right) {
        return left.doubleValue() >= right.doubleValue();
    }

    @Override
    public boolean isLessThan(Number left, Number right) {
        return left.doubleValue() < right.doubleValue();
    }

    @Override
    public boolean isLessThanOrEqual(Number left, Number right) {
        return left.doubleValue() <= right.doubleValue();
    }

    @Override
    public boolean isEqual(Number left, Number right) {
        return left.doubleValue() == right.doubleValue();
    }

    @Override
    public Number diff(Number left, Number right) {
        return left.doubleValue() - right.doubleValue();
    }

    @Override
    public Number scaleTo(Number value, int scale) {
        return BigDecimal.valueOf(value.doubleValue()).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
