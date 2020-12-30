package com.sbss.bithon.collector.datasource.typing;

/**
 * @author
 * @date
 */
public class DoubleValueType implements IValueType {

    public final static IValueType INSTANCE = new DoubleValueType();

    @Override
    public String format(Number value) {
        return String.format("%.2f", value.doubleValue());
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
}
