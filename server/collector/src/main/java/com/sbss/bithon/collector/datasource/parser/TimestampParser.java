package com.sbss.bithon.collector.datasource.parser;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.*;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Author: frank.chen021@outlook.com
 * Date: 2020/11/30 5:49 下午
 */
public class TimestampParser {
    public static Function<String, DateTime> createTimestampParser(
        final String format
    ) {
        if ("auto".equalsIgnoreCase(format)) {
            // Could be iso or millis
            final DateTimes.UtcFormatter parser = DateTimes.wrapFormatter(createAutoParser());
            return (String input) -> {
                Preconditions.checkArgument(!Strings.isNullOrEmpty(input), "null timestamp");

                for (int i = 0; i < input.length(); i++) {
                    if (input.charAt(i) < '0' || input.charAt(i) > '9') {
                        input = ParserUtils.stripQuotes(input);
                        int lastIndex = input.lastIndexOf(' ');
                        DateTimeZone timeZone = DateTimeZone.UTC;
                        if (lastIndex > 0) {
                            DateTimeZone timeZoneFromString = ParserUtils.getDateTimeZone(input.substring(lastIndex + 1));
                            if (timeZoneFromString != null) {
                                timeZone = timeZoneFromString;
                                input = input.substring(0, lastIndex);
                            }
                        }

                        return parser.parse(input).withZone(timeZone);
                    }
                }

                return DateTimes.utc(Long.parseLong(input));
            };
        } else if ("iso".equalsIgnoreCase(format)) {
            return input -> {
                Preconditions.checkArgument(!Strings.isNullOrEmpty(input), "null timestamp");
                return DateTimes.of(ParserUtils.stripQuotes(input));
            };
        } else if ("posix".equalsIgnoreCase(format)
            || "millis".equalsIgnoreCase(format)
            || "micro".equalsIgnoreCase(format)
            || "nano".equalsIgnoreCase(format)) {
            final Function<Number, DateTime> numericFun = createNumericTimestampParser(format);
            return input -> {
                Preconditions.checkArgument(!Strings.isNullOrEmpty(input), "null timestamp");
                return numericFun.apply(Long.parseLong(ParserUtils.stripQuotes(input)));
            };
        } else if ("ruby".equalsIgnoreCase(format)) {
            final Function<Number, DateTime> numericFun = createNumericTimestampParser(format);
            return input -> {
                Preconditions.checkArgument(!Strings.isNullOrEmpty(input), "null timestamp");
                return numericFun.apply(Double.parseDouble(ParserUtils.stripQuotes(input)));
            };
        } else {
            try {
                final DateTimes.UtcFormatter formatter = DateTimes.wrapFormatter(DateTimeFormat.forPattern(format));
                return input -> {
                    Preconditions.checkArgument(!Strings.isNullOrEmpty(input), "null timestamp");
                    return formatter.parse(ParserUtils.stripQuotes(input));
                };
            } catch (Exception e) {
                throw new RuntimeException(String.format("Unable to parse timestamps with format [%s]", format));
            }
        }
    }

    public static Function<Number, DateTime> createNumericTimestampParser(
        final String format
    ) {
        if ("posix".equalsIgnoreCase(format)) {
            return input -> DateTimes.utc(TimeUnit.SECONDS.toMillis(input.longValue()));
        } else if ("micro".equalsIgnoreCase(format)) {
            return input -> DateTimes.utc(TimeUnit.MICROSECONDS.toMillis(input.longValue()));
        } else if ("nano".equalsIgnoreCase(format)) {
            return input -> DateTimes.utc(TimeUnit.NANOSECONDS.toMillis(input.longValue()));
        } else if ("ruby".equalsIgnoreCase(format)) {
            return input -> DateTimes.utc(Double.valueOf(input.doubleValue() * 1000).longValue());
        } else {
            return input -> DateTimes.utc(input.longValue());
        }
    }

    public static Function<Object, DateTime> createObjectTimestampParser(
        final String format
    ) {
        final Function<String, DateTime> stringFun = createTimestampParser(format);
        final Function<Number, DateTime> numericFun = createNumericTimestampParser(format);
        final boolean isNumericFormat = isNumericFormat(format);

        return o -> {
            Preconditions.checkNotNull(o, "null timestamp");

            if (o instanceof Number && isNumericFormat) {
                return numericFun.apply((Number) o);
            } else {
                return stringFun.apply(o.toString());
            }
        };
    }

    private static boolean isNumericFormat(String format) {
        return "auto".equalsIgnoreCase(format)
            || "millis".equalsIgnoreCase(format)
            || "posix".equalsIgnoreCase(format)
            || "micro".equalsIgnoreCase(format)
            || "nano".equalsIgnoreCase(format)
            || "ruby".equalsIgnoreCase(format);
    }

    private static DateTimeFormatter createAutoParser() {
        final DateTimeFormatter offsetElement = new DateTimeFormatterBuilder()
            .appendTimeZoneOffset("Z", true, 2, 4)
            .toFormatter();

        DateTimeParser timeOrOffset = new DateTimeFormatterBuilder()
            .append(
                null,
                new DateTimeParser[]{
                    new DateTimeFormatterBuilder().appendLiteral('T').toParser(),
                    new DateTimeFormatterBuilder().appendLiteral(' ').toParser()
                }
            )
            .appendOptional(ISODateTimeFormat.timeElementParser().getParser())
            .appendOptional(offsetElement.getParser())
            .toParser();

        return new DateTimeFormatterBuilder()
            .append(ISODateTimeFormat.dateElementParser())
            .appendOptional(timeOrOffset)
            .toFormatter();
    }
}
