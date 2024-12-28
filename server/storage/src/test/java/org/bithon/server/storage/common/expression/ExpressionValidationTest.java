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

package org.bithon.server.storage.common.expression;

import org.bithon.component.commons.expression.function.Functions;
import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.time.TimeSpan;
import org.bithon.server.storage.datasource.DefaultSchema;
import org.bithon.server.storage.datasource.TimestampSpec;
import org.bithon.server.storage.datasource.column.LongColumn;
import org.bithon.server.storage.datasource.column.StringColumn;
import org.junit.Assert;
import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;

/**
 * @author Frank Chen
 * @date 21/1/24 1:27 pm
 */
public class ExpressionValidationTest {

    private final DefaultSchema schema = new DefaultSchema(
        "schema",
        "schema",
        new TimestampSpec("timestamp"),
        Arrays.asList(new StringColumn("a", "a"), new LongColumn("intB", "intB")),
        Collections.emptyList()
    );

    @Test
    public void test_IdentifierValidationInFunction() {
        // The identifier 'a' is defined in the schema
        ExpressionASTBuilder.builder()
                            .schema(schema)
                            .functions(Functions.getInstance())
                            .build("hasToken(a, 'a')");

        Assert.assertThrows(ExpressionValidationException.class,
                            () -> ExpressionASTBuilder.builder()
                                                      .schema(schema)
                                                      .functions(Functions.getInstance())
                                                      .build("hasToken(no_exist_column, 'a')"));
    }

    @Test
    public void test_NumberToStringImplicitConversion() {
        Assert.assertEquals("a > '5'",
                            ExpressionASTBuilder.builder()
                                                .schema(schema)
                                                .build("5 < a")
                                                .serializeToText(null));

        Assert.assertEquals("a > '5'", ExpressionASTBuilder.builder()
                                                           .schema(schema)
                                                           .build("a > 5")
                                                           .serializeToText(null));

        Assert.assertEquals("a > '5.3'",
                            ExpressionASTBuilder.builder()
                                                .schema(schema)
                                                .build("5.3 < a")
                                                .serializeToText(null));
    }

    @Test
    public void test_StringToIntImplicitConversion() {
        // String ---> Int
        Assert.assertEquals("intB > 5", ExpressionASTBuilder.builder()
                                                            .schema(schema)
                                                            .build("intB > '5'")
                                                            .serializeToText(null));

        Assert.assertThrows(ExpressionValidationException.class,
                            () -> ExpressionASTBuilder.builder()
                                                      .schema(schema)
                                                      .build("intB > 'valid'")
                                                      .serializeToText(null));

    }

    @Test
    public void test_StringToDateTimeImplicitConversion() throws ParseException {
        String dateTime = "2023-01-04 00:00:00";
        long timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(dateTime).getTime();

        Assert.assertEquals(StringUtils.format("timestamp > %d", timestamp),

                            ExpressionASTBuilder.builder()
                                                .schema(schema)
                                                .build("timestamp > '2023-01-04 00:00:00'")
                                                .serializeToText(null));

        Assert.assertThrows(ExpressionValidationException.class,
                            () -> ExpressionASTBuilder.builder()
                                                      .schema(schema)
                                                      .build("timestamp > 'invalid'")
                                                      .serializeToText(null));

        Assert.assertThrows(ExpressionValidationException.class,
                            () -> ExpressionASTBuilder.builder()
                                                      .schema(schema)
                                                      .build("timestamp > not_defined")
                                                      .serializeToText(null));
    }

    @Test
    public void test_LongToDateTimeImplicitConversion() {
        // Long ---> DateTime
        TimeSpan timeSpan = TimeSpan.fromISO8601("2023-01-04T00:00:00.000+08:00");
        Assert.assertEquals(StringUtils.format("timestamp > %d", timeSpan.getMilliseconds()),
                            ExpressionASTBuilder.builder()
                                                .schema(schema)
                                                .build("timestamp > " + timeSpan.getMilliseconds())
                                                .serializeToText(null));
    }

    @Test
    public void test_TypeMismatchForIdentifier() {
        Assert.assertThrows(ExpressionValidationException.class,
                            () ->
                                ExpressionASTBuilder.builder()
                                                    .schema(schema)
                                                    .build("a > intB")
        );
    }

    @Test
    public void test_UncompletedLogicalExpression() {
        Assert.assertThrows(ExpressionValidationException.class,
                            () ->
                                ExpressionASTBuilder.builder()
                                                    .schema(schema)
                                                    .build("a = 'INFO' and a")
        );
    }
}
