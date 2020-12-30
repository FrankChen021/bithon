package com.sbss.bithon.agent.core.utils.time;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 5:20 下午
 */
public class DateTime {

    public static String toISO8601(long milliSeconds) {
        return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX").format(new Date(milliSeconds));
    }
}
