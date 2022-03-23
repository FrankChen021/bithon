package org.bithon.component.commons.utils;

/**
 * @author Frank Chen
 * @date 23/3/22 2:38 PM
 */
public class Arguments {
    public static <T> T checkNotNull(String name, T value) {
        if (value == null) {
            throw new InvalidArgumentException("parameter [%s] should not be NULL", name);
        }
        return value;
    }

    public static class InvalidArgumentException extends RuntimeException {
        public InvalidArgumentException(String format, Object... args) {
            super(StringUtils.format(format, args));
        }
    }
}
