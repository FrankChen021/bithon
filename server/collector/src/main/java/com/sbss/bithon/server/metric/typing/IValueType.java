package com.sbss.bithon.server.metric.typing;

/**
 * @author frank.chen021@outlook.com
 */
public interface IValueType {
    String format(Number value);

    boolean isGreaterThan(Number left, Number right);

    boolean isGreaterThanOrEqual(Number left, Number right);

    boolean isLessThan(Number left, Number right);

    boolean isLessThanOrEqual(Number left, Number right);

    boolean isEqual(Number left, Number right);

    Number diff(Number left, Number right);
    Number scaleTo(Number value, int scale);
}
