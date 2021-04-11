package com.sbss.bithon.server.common.utils.datetime;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/11 18:33
 */
public class Period {
    @Getter
    private final long milliseconds;

    @JsonCreator
    public Period(String period) {
        milliseconds = org.joda.time.Period.parse(period).toStandardDuration().getMillis();
    }

    public static Period years(int i) {
        return new Period(String.format("P%dY", i));
    }
}
