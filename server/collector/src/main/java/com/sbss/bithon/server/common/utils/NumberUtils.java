package com.sbss.bithon.server.common.utils;

import java.math.BigDecimal;

/**
 * @author frankchen
 * @Date 2020-08-27 17:28:11
 */
public class NumberUtils {

    public static double div(double dividend,
                             double divisor,
                             int scale) {
        try {
            return new BigDecimal(dividend / divisor).setScale(scale, BigDecimal.ROUND_HALF_UP).doubleValue();
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    public static double div(BigDecimal dividend,
                             BigDecimal divisor,
                             int scale) {
        if (divisor.equals(BigDecimal.ZERO)) {
            return dividend.doubleValue();
        }
        return dividend.divide(divisor, scale, BigDecimal.ROUND_HALF_UP).doubleValue();
    }

    public static BigDecimal scaleTo(double val,
                                     int scale) {
        return new BigDecimal(val).setScale(scale, BigDecimal.ROUND_HALF_UP);
    }

    public static String toString(double val,
                                  int scale) {
        return BigDecimal.valueOf(val).setScale(scale, BigDecimal.ROUND_HALF_UP).toString();
    }
}
