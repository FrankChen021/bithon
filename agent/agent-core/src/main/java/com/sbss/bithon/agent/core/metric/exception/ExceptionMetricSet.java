package com.sbss.bithon.agent.core.metric.exception;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/16 8:21 下午
 */
public class ExceptionMetricSet {
    // dimension
    private final String uri;
    private final String exceptionId;
    private final String exceptionClass;
    private final String message;
    private final String stackTrace;
    // counter
    private int count = 0;

    public ExceptionMetricSet(String uri,
                              String exceptionClass,
                              String message,
                              String stackTrace) {
        this.uri = uri;
        this.exceptionId = md5(stackTrace) + md5(message);
        this.message = message;
        this.exceptionClass = exceptionClass;
        this.stackTrace = stackTrace;
    }

    public static ExceptionMetricSet fromException(String uri, Throwable exception) {
        return new ExceptionMetricSet(uri,
                                      exception.getClass().getSimpleName(),
                                      exception.getMessage(),
                                      getFullStack(exception.getStackTrace()));
    }

    private static String md5(String stack) {
        if (stack == null) {
            return null;
        }
        try {
            byte[] byteArray = stack.getBytes(StandardCharsets.UTF_8);
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            md5.update(byteArray);
            byte[] bytes = md5.digest();
            final char[] HEX_DIGITS = "0123456789ABCDEF".toCharArray();
            StringBuilder ret = new StringBuilder(bytes.length * 2);
            for (byte aByte : bytes) {
                ret.append(HEX_DIGITS[(aByte >> 4) & 0x0f]);
                ret.append(HEX_DIGITS[aByte & 0x0f]);
            }
            return ret.toString();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static String getFullStack(StackTraceElement[] stacks) {
        StringBuilder sb = new StringBuilder();
        if (stacks != null && stacks.length > 0) {
            for (StackTraceElement msg : stacks) {
                sb.append(msg.toString()).append("\r\n");
            }
        }
        return sb.toString();
    }

    public String getUri() {
        return uri;
    }

    public String getExceptionId() {
        return exceptionId;
    }

    public String getExceptionClass() {
        return exceptionClass;
    }

    public String getStackTrace() {
        return stackTrace;
    }

    public int getCount() {
        return count;
    }

    public String getMessage() {
        return message;
    }

    public void incrCount() {
        count++;
    }
}
