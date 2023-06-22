/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.storage.jdbc.utils;

import org.bithon.component.commons.utils.Preconditions;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.matcher.BetweenMatcher;
import org.bithon.server.commons.matcher.GreaterThanMatcher;
import org.bithon.server.commons.matcher.GreaterThanOrEqualMatcher;
import org.bithon.server.commons.matcher.IMatcherVisitor;
import org.bithon.server.commons.matcher.InMatcher;
import org.bithon.server.commons.matcher.LessThanMatcher;
import org.bithon.server.commons.matcher.LessThanOrEqualMatcher;
import org.bithon.server.commons.matcher.NotEqualMatcher;
import org.bithon.server.commons.matcher.StringAntPathMatcher;
import org.bithon.server.commons.matcher.StringContainsMatcher;
import org.bithon.server.commons.matcher.StringEndWithMatcher;
import org.bithon.server.commons.matcher.StringEqualMatcher;
import org.bithon.server.commons.matcher.StringIContainsMatcher;
import org.bithon.server.commons.matcher.StringLikeMatcher;
import org.bithon.server.commons.matcher.StringRegexMatcher;
import org.bithon.server.commons.matcher.StringStartsWithMatcher;
import org.bithon.server.storage.datasource.DataSourceSchema;
import org.bithon.server.storage.datasource.IColumnSpec;
import org.bithon.server.storage.datasource.typing.IValueType;
import org.bithon.server.storage.datasource.typing.StringValueType;
import org.bithon.server.storage.metrics.DimensionFilter;
import org.bithon.server.storage.metrics.IFilter;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * build SQL where clause
 *
 * @author frank.chenling
 */
public class SQLFilterBuilder implements IMatcherVisitor<String> {

    public static String build(DataSourceSchema schema, Collection<IFilter> filters) {
        return build(schema, filters.stream());
    }

    public static String build(DataSourceSchema schema, Stream<IFilter> filterStream) {
        return build(schema, filterStream, true);
    }

    public static String build(DataSourceSchema schema, Stream<IFilter> filterStream, boolean useQualifiedName) {
        return filterStream.map(filter -> filter.getMatcher()
                                                .accept(new SQLFilterBuilder(schema, filter, useQualifiedName)))
                           .collect(Collectors.joining(" AND "));
    }

    private final String fieldName;
    private final IValueType valueType;

    public SQLFilterBuilder(DataSourceSchema schema, IFilter filter) {
        this(schema, filter, true);
    }

    public SQLFilterBuilder(DataSourceSchema schema, IFilter filter, boolean useQualifiedName) {
        IColumnSpec columnSpec;
        if (IFilter.TYPE_DIMENSION.equals(filter.getType())) {
            String nameType = ((DimensionFilter) filter).getNameType();
            if ("name".equals(nameType)) {
                columnSpec = schema.getDimensionSpecByName(filter.getName());
            } else if ("alias".equals(nameType)) {
                columnSpec = schema.getDimensionSpecByAlias(filter.getName());
            } else {
                columnSpec = null;
            }
            Preconditions.checkNotNull(columnSpec, "dimension [%s] is not defined in data source [%s]", filter.getName(), schema.getName());
        } else {
            columnSpec = schema.getMetricSpecByName(filter.getName());
            Preconditions.checkNotNull(columnSpec, "metric [%s] is not defined in data source [%s]", filter.getName(), schema.getName());
        }

        this.fieldName = formatName(useQualifiedName, true, schema.getDataStoreSpec().getStore(), columnSpec.getName());
        this.valueType = columnSpec.getValueType();
    }

    private static String formatName(boolean useQualifiedName, boolean quoted, String table, String field) {
        if (useQualifiedName) {
            return quoted ? StringUtils.format("\"%s\".\"%s\"", table, field) : StringUtils.format("%s.%s", table, field);
        } else {
            return quoted ? StringUtils.format("\"%s\"", field) : field;
        }
    }

    /**
     * @param quoted Whether a field SHOULD be quoted.
     *               For filters on the tag field, the field name is sth like tags['htt_method'],
     *               this name tags['htt_method'] SHOULD be quoted or the whole text would be treated as a column by database
     */
    public SQLFilterBuilder(String table,
                            String fieldName,
                            IValueType valueType,
                            boolean quoted,
                            boolean useQualifiedName) {
        this.valueType = valueType;
        this.fieldName = formatName(useQualifiedName, quoted, table, fieldName);
    }

    @Override
    public String visit(StringEqualMatcher matcher) {
        return StringUtils.format("%s = '%s'",
                                  fieldName,
                                  matcher.getPattern());
    }

    @Override
    public String visit(NotEqualMatcher matcher) {
        return StringUtils.format("%s <> '%s'",
                                  fieldName,
                                  matcher.getValue());
    }

    @Override
    public String visit(StringAntPathMatcher matcher) {
        throw new RuntimeException("Not Supported Now");
    }

    @Override
    public String visit(StringContainsMatcher matcher) {
        return StringUtils.format("%s LIKE '%%%s%%'", fieldName, matcher.getPattern());
    }

    @Override
    public String visit(StringEndWithMatcher matcher) {
        throw new RuntimeException("Not Supported Now");
    }

    @Override
    public String visit(StringIContainsMatcher matcher) {
        throw new RuntimeException("Not Supported Now");
    }

    @Override
    public String visit(StringRegexMatcher matcher) {
        throw new RuntimeException("Not Supported Now");
    }

    @Override
    public String visit(StringStartsWithMatcher matcher) {
        throw new RuntimeException("Not Supported Now");
    }

    @Override
    public String visit(BetweenMatcher matcher) {
        return StringUtils.format("%s BETWEEN %s AND %s",
                                  fieldName,
                                  matcher.getLower().toString(),
                                  matcher.getUpper().toString());
    }

    @Override
    public String visit(InMatcher inMatcher) {
        if (inMatcher.getPattern().size() == 1) {
            return StringUtils.format("%s = '%s'",
                                      fieldName,
                                      inMatcher.getPattern().iterator().next());
        }

        return StringUtils.format("%s in (%s)",
                                  fieldName,
                                  inMatcher.getPattern()
                                           .stream()
                                           .map((item) -> "'" + item + "'")
                                           .collect(Collectors.joining(",")));
    }

    /**
     * Use the full qualified name because there might be an aggregated field with the same name as the field name
     */
    @Override
    public String visit(GreaterThanMatcher matcher) {
        String pattern;
        if (valueType instanceof StringValueType) {
            pattern = "%s > '%s'";
        } else {
            pattern = "%s > %s";
        }
        return StringUtils.format(pattern, fieldName, matcher.getValue().toString());
    }

    @Override
    public String visit(GreaterThanOrEqualMatcher matcher) {
        String pattern;
        if (valueType instanceof StringValueType) {
            pattern = "%s >= '%s'";
        } else {
            pattern = "%s >= %s";
        }
        return StringUtils.format(pattern, fieldName, matcher.getValue().toString());
    }

    @Override
    public String visit(LessThanMatcher matcher) {
        String pattern;
        if (valueType instanceof StringValueType) {
            pattern = "%s < '%s'";
        } else {
            pattern = "%s < %s";
        }
        return StringUtils.format(pattern,
                                  fieldName,
                                  matcher.getValue().toString());
    }

    @Override
    public String visit(LessThanOrEqualMatcher matcher) {
        String pattern;
        if (valueType instanceof StringValueType) {
            pattern = "%s <= '%s'";
        } else {
            pattern = "%s <= %s";
        }
        return StringUtils.format(pattern,
                                  fieldName,
                                  matcher.getValue().toString());
    }

    @Override
    public String visit(StringLikeMatcher matcher) {
        return StringUtils.format("%s LIKE '%s'", fieldName, matcher.getPattern());
    }
}
