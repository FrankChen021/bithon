package com.sbss.bithon.server.metric;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sbss.bithon.server.metric.parser.TimestampParser;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

/**
 * Author: frank.chen021@outlook.com
 * Date: 2020/11/30 5:48 下午
 */
public class TimestampSpec {
    public static final String DEFAULT_COLUMN = "timestamp";
    // remember last value parsed
    private static final ThreadLocal<ParseCtx> PARSE_CTX = ThreadLocal.withInitial(ParseCtx::new);
    private static final String DEFAULT_FORMAT = "auto";
    private final String timestampColumn;
    private final String timestampFormat;
    /**
     * This field is a derivative of {@link #timestampFormat}; not checked in {@link #equals} and {@link #hashCode}
     */
    private final Function<Object, DateTime> timestampConverter;
    @JsonCreator
    public TimestampSpec(
        @JsonProperty("column") @Nullable String timestampColumn,
        @JsonProperty("format") @Nullable String format,
        // this value should never be set for production data; the data loader uses it before a timestamp column is chosen
        @JsonProperty("missingValue") @Nullable DateTime missingValue
    ) {
        this.timestampColumn = (timestampColumn == null) ? DEFAULT_COLUMN : timestampColumn;
        this.timestampFormat = format == null ? DEFAULT_FORMAT : format;
        this.timestampConverter = TimestampParser.createObjectTimestampParser(timestampFormat);
    }

    //simple merge strategy on timestampSpec that checks if all are equal or else
    //returns null. this can be improved in future but is good enough for most use-cases.
    public static TimestampSpec mergeTimestampSpec(List<TimestampSpec> toMerge) {
        if (toMerge == null || toMerge.size() == 0) {
            return null;
        }

        TimestampSpec result = toMerge.get(0);
        for (int i = 1; i < toMerge.size(); i++) {
            if (toMerge.get(i) == null) {
                continue;
            }
            if (!Objects.equals(result, toMerge.get(i))) {
                return null;
            }
        }

        return result;
    }

    @JsonProperty("column")
    public String getTimestampColumn() {
        return timestampColumn;
    }

    @JsonProperty("format")
    public String getTimestampFormat() {
        return timestampFormat;
    }

    @Nullable
    public DateTime extractTimestamp(@Nullable Map<String, Object> input) {
        return parseDateTime(getRawTimestamp(input));
    }

    @Nullable
    public DateTime parseDateTime(@Nullable Object input) {
        DateTime extracted = null;
        if (input != null) {
            ParseCtx ctx = PARSE_CTX.get();
            // Check if the input is equal to the last input, so we don't need to parse it again
            if (input.equals(ctx.lastTimeObject)) {
                extracted = ctx.lastDateTime;
            } else {
                extracted = timestampConverter.apply(input);
                ParseCtx newCtx = new ParseCtx();
                newCtx.lastTimeObject = input;
                newCtx.lastDateTime = extracted;
                PARSE_CTX.set(newCtx);
            }
        }
        return extracted;
    }

    @Nullable
    public Object getRawTimestamp(@Nullable Map<String, Object> input) {
        return input == null ? null : input.get(timestampColumn);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimestampSpec that = (TimestampSpec) o;

        if (!timestampColumn.equals(that.timestampColumn)) {
            return false;
        }
        if (!timestampFormat.equals(that.timestampFormat)) {
            return false;
        }
        return true;

    }

    @Override
    public int hashCode() {
        int result = timestampColumn.hashCode();
        result = 31 * result + timestampFormat.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TimestampSpec{" +
               "timestampColumn='" + timestampColumn + '\'' +
               ", timestampFormat='" + timestampFormat + '\'' +
               '}';
    }

    private static class ParseCtx {
        Object lastTimeObject = null;
        DateTime lastDateTime = null;
    }
}
