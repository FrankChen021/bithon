package org.bithon.component.commons.utils;

/**
 * @author Frank Chen
 * @date 23/3/22 2:38 PM
 */
public class Preconditions {
    public static <T> T checkArgumentNotNull(String name, T value) {
        if (value == null) {
            throw new InvalidValueException("parameter [%s] should not be NULL", name);
        }
        return value;
    }

    public static <T> T checkNotNull(T value, String message) {
        if (value == null) {
            throw new InvalidValueException(message);
        }
        return value;
    }

    public static class InvalidValueException extends RuntimeException {
        public InvalidValueException(String message) {
            super(message);
        }

        public InvalidValueException(String format, Object... args) {
            super(StringUtils.format(format, args));
        }
    }
}
