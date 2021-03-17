package com.sbss.bithon.server.metric.typing;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/28
 */
public class StringValueType implements IValueType {

    public static StringValueType INSTANCE = new StringValueType();

    @Override
    public String format(Number value) {
        return null;
    }

    @Override
    public boolean isGreaterThan(Number left, Number right) {
        return false;
    }

    @Override
    public boolean isGreaterThanOrEqual(Number left, Number right) {
        return false;
    }

    @Override
    public boolean isLessThan(Number left, Number right) {
        return false;
    }

    @Override
    public boolean isLessThanOrEqual(Number left, Number right) {
        return false;
    }

    @Override
    public boolean isEqual(Number left, Number right) {
        return false;
    }

    @Override
    public Number diff(Number left, Number right) {
        return null;
    }

    @Override
    public Number scaleTo(Number value, int scale) {
        return value;
    }
}
