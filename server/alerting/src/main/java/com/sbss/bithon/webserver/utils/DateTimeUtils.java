package com.sbss.bithon.webserver.utils;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/12 9:39 下午
 */
public class DateTimeUtils {

    public static long dropMilliseconds(long timestamp) {
        return timestamp / 1000 * 1000;
    }
}
