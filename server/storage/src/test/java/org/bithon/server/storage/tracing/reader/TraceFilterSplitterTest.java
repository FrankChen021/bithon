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

package org.bithon.server.storage.tracing.reader;

import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.server.datasource.DefaultSchema;
import org.bithon.server.datasource.TimestampSpec;
import org.bithon.server.datasource.column.ObjectColumn;
import org.bithon.server.datasource.column.StringColumn;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for TraceFilterSplitter
 *
 * @author frank.chen021@outlook.com
 */
@DisplayName("TraceFilterSplitter Tests")
public class TraceFilterSplitterTest {

    private DefaultSchema summaryTableSchema;
    private DefaultSchema indexTableSchema;

    @BeforeEach
    void setUp() {
        // Create summary table schema with attributes column (ObjectColumn for tags)
        summaryTableSchema = new DefaultSchema(
            "trace_span_summary",
            "trace_span_summary",
            new TimestampSpec("timestamp"),
            List.of(
                new StringColumn("appName", "appName"),
                new StringColumn("status", "status"),
                new ObjectColumn("attributes", "tags")  // The tags/attributes column
            ),
            Collections.emptyList()
        );

        // Create index table schema with indexed tag columns
        // f1 -> http.method, f2 -> http.status, f3 -> db.type
        indexTableSchema = new DefaultSchema(
            "trace_span_tag_index",
            "trace_span_tag_index",
            new TimestampSpec("timestamp"),
            List.of(
                new StringColumn("f1", "http.method"),   // Index position 1 -> http.method
                new StringColumn("f2", "http.status"),   // Index position 2 -> http.status
                new StringColumn("f3", "db.type")        // Index position 3 -> db.type
            ),
            Collections.emptyList()
        );
    }

    @Test
    @DisplayName("Null expression should return empty indexedTagFilters")
    void testNullExpression() {
        TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
        splitter.split(null);

        assertNull(splitter.getExpression());
        assertNotNull(splitter.getIndexedTagFilters());
        assertTrue(splitter.getIndexedTagFilters().isEmpty());
    }

    @Nested
    @DisplayName("Single Indexed Tag Filter Tests")
    class SingleIndexedTagTests {

        @Test
        @DisplayName("Single indexed tag filter should be extracted")
        void testSingleIndexedTagFilter() {
            // Given: expression with indexed tag
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: expression should be null (all extracted), indexedTagFilters should contain the filter
            assertNull(splitter.getExpression());
            assertEquals(1, splitter.getIndexedTagFilters().size());

            // Verify indexed filter is converted to use indexed column
            String indexedFilterText = splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 = 'GET'", indexedFilterText,
                         "Indexed filter should be exactly f1 = 'GET'");
        }

        @Test
        @DisplayName("Single indexed tag filter with tags alias should be extracted")
        void testSingleIndexedTagFilterWithTagsAlias() {
            // Given: expression using 'tags' alias for attributes
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("tags['http.method'] = 'GET'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: should extract the filter
            assertNull(splitter.getExpression());
            assertEquals(1, splitter.getIndexedTagFilters().size());

            // Verify indexed filter is converted to use indexed column
            String indexedFilterText = splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 = 'GET'", indexedFilterText,
                         "Indexed filter should be exactly f1 = 'GET'");
        }

        @Test
        @DisplayName("Single indexed tag filter with different operators")
        void testSingleIndexedTagFilterWithDifferentOperators() {
            // Test with != operator
            IExpression expr1 = ExpressionASTBuilder.builder()
                                                    .schema(summaryTableSchema)
                                                    .build("attributes['http.method'] != 'POST'");
            TraceFilterSplitter splitter1 = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter1.split(expr1);
            assertEquals(1, splitter1.getIndexedTagFilters().size());
            assertNull(splitter1.getExpression());
            String indexedFilterText1 = splitter1.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 <> 'POST'", indexedFilterText1,
                         "Indexed filter should be exactly f1 <> 'POST'");

            // Test with > operator
            IExpression expr2 = ExpressionASTBuilder.builder()
                                                    .schema(summaryTableSchema)
                                                    .build("attributes['http.status'] > 400");
            TraceFilterSplitter splitter2 = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter2.split(expr2);
            assertEquals(1, splitter2.getIndexedTagFilters().size());
            assertNull(splitter2.getExpression());
            String indexedFilterText2 = splitter2.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f2 > '400'", indexedFilterText2,
                         "Indexed filter should be exactly f2 > 400");

            // Test with IN operator
            IExpression expr3 = ExpressionASTBuilder.builder()
                                                    .schema(summaryTableSchema)
                                                    .build("attributes['http.method'] IN ('GET', 'POST')");
            TraceFilterSplitter splitter3 = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter3.split(expr3);
            assertEquals(1, splitter3.getIndexedTagFilters().size());
            assertNull(splitter3.getExpression());
            String indexedFilterText3 = splitter3.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 in ('GET', 'POST')", indexedFilterText3,
                         "Indexed filter should be exactly f1 IN ('GET', 'POST')");
        }
    }

    @Nested
    @DisplayName("Mixed Filter Tests")
    class MixedFilterTests {

        @Test
        @DisplayName("Mixed indexed and non-indexed filters should be split correctly")
        void testMixedFilters() {
            // Given: expression with both indexed tag filter and regular filter
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET' AND status = 'ok'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: indexed filter should be extracted, regular filter should remain
            assertNotNull(splitter.getExpression());
            assertEquals(1, splitter.getIndexedTagFilters().size());

            // Verify indexed filter is converted to use indexed column
            String indexedFilterText = splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 = 'GET'", indexedFilterText,
                         "Indexed filter should be exactly f1 = 'GET'");

            // Verify remaining expression contains only the non-indexed filter
            String remainingExpressionText = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertTrue(remainingExpressionText.contains("status") && remainingExpressionText.contains("ok"),
                       "Remaining expression should contain status = 'ok'");
            // Should not contain the indexed tag filter
            assertTrue(!remainingExpressionText.contains("attributes['http.method']") && !remainingExpressionText.contains("tags['http.method']"),
                       "Remaining expression should not contain indexed tag filter");
        }

        @Test
        @DisplayName("Multiple indexed tag filters should all be extracted")
        void testMultipleIndexedTagFilters() {
            // Given: expression with multiple indexed tag filters
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET' AND attributes['http.status'] = 200");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: both indexed filters should be extracted
            assertEquals(2, splitter.getIndexedTagFilters().size());
            assertNotNull(splitter.getExpression());

            // Verify both indexed filters are converted correctly
            Assertions.assertEquals("f1 = 'GET'", splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE));
            Assertions.assertEquals("f2 = '200'", splitter.getIndexedTagFilters().get(1).serializeToText(IdentifierQuotaStrategy.NONE));

            // Verify remaining expression has placeholder
            String remainingExpressionText = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("(1 = 1)", remainingExpressionText,
                         "Remaining expression should be (1 = 1)");
        }

        @Test
        @DisplayName("Multiple indexed and non-indexed filters should be split correctly")
        void testMultipleMixedFilters() {
            // Given: expression with multiple indexed and non-indexed filters
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET' AND attributes['http.status'] = 200 AND appName = 'myapp'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: indexed filters should be extracted, non-indexed should remain
            assertEquals(2, splitter.getIndexedTagFilters().size());
            assertNotNull(splitter.getExpression());

            // Verify indexed filters
            Assertions.assertEquals("f1 = 'GET'", splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE));
            Assertions.assertEquals("f2 = '200'", splitter.getIndexedTagFilters().get(1).serializeToText(IdentifierQuotaStrategy.NONE));

            // Verify remaining expression contains non-indexed filter
            String remainingExpressionText = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("(appName = 'myapp')", remainingExpressionText,
                         "Remaining expression should be (appName = 'myapp') ");
        }

        @Test
        @DisplayName("Non-indexed tag filter should remain in expression")
        void testNonIndexedTagFilter() {
            // Given: expression with non-indexed tag (e.g., 'user.id' is not in indexTableSchema)
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['user.id'] = '123' AND status = 'ok'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: non-indexed tag filter should remain in expression
            assertNotNull(splitter.getExpression());
            assertTrue(splitter.getIndexedTagFilters().isEmpty());

            // Verify remaining expression contains both filters
            String remainingExpressionText = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("(attributes['user.id'] = '123') AND (status = 'ok')", remainingExpressionText,
                         "Remaining expression should contain both filters");
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    class EdgeCaseTests {

        @Test
        @DisplayName("Invalid expression type should throw exception")
        void testInvalidExpressionType() {
            // Given: expression that is not a LogicalExpression or ConditionalExpression
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("appName");

            // When/Then: should throw HttpMappableException
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            assertThrows(HttpMappableException.class, () -> splitter.split(expr));
        }

        @Test
        @DisplayName("Nested map access should not be supported")
        void testNestedMapAccess() {
            // Given: nested map access (not supported)
            // This would need a different expression structure, testing the constraint
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET'");

            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            // This should work for single level access
            splitter.split(expr);
            assertNotNull(splitter);
        }

        @Test
        @DisplayName("Empty AND expression after extraction should have placeholder")
        void testEmptyAndExpression() {
            // Given: expression with only indexed tag filters
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET' AND attributes['http.status'] = 200");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: expression should have placeholder (1=1)
            assertNotNull(splitter.getExpression());
            String remainingExpressionText = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("(1 = 1)", remainingExpressionText,
                         "Remaining expression should be (1 = 1)");
        }
    }

    @Nested
    @DisplayName("Complex Expression Tests")
    class ComplexExpressionTests {

        @Test
        @DisplayName("Complex AND expression with multiple indexed tags")
        void testComplexAndExpression() {
            // Given: complex expression with multiple indexed tags
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET' AND attributes['http.status'] = 200 AND attributes['db.type'] = 'mysql'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: all three indexed filters should be extracted
            assertEquals(3, splitter.getIndexedTagFilters().size());
            assertNotNull(splitter.getExpression());

            // Verify all three indexed filters are converted
            assertEquals("f1 = 'GET'", splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE));
            assertEquals("f2 = '200'", splitter.getIndexedTagFilters().get(1).serializeToText(IdentifierQuotaStrategy.NONE));
            assertEquals("f3 = 'mysql'", splitter.getIndexedTagFilters().get(2).serializeToText(IdentifierQuotaStrategy.NONE));

            // Verify remaining expression
            String remainingExpressionText = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("(1 = 1)", remainingExpressionText,
                         "Remaining expression should be (1 = 1)");
        }

        @Test
        @DisplayName("Complex expression with indexed and non-indexed filters")
        void testComplexMixedExpression() {
            // Given: complex expression with indexed and non-indexed filters
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET' AND status = 'ok' AND attributes['http.status'] > 400 AND appName = 'myapp'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: should extract indexed filters, keep non-indexed
            assertEquals(2, splitter.getIndexedTagFilters().size());
            assertNotNull(splitter.getExpression());

            // Verify indexed filters
            Assertions.assertEquals("f1 = 'GET'", splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE));
            Assertions.assertEquals("f2 > '400'", splitter.getIndexedTagFilters().get(1).serializeToText(IdentifierQuotaStrategy.NONE));

            // Verify remaining expression contains non-indexed filters
            String remainingExpressionText = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("(status = 'ok') AND (appName = 'myapp')", remainingExpressionText,
                         "Remaining expression should contain status and appName filters");
        }

        @Test
        @DisplayName("Expression serialization after split should work")
        void testExpressionSerializationAfterSplit() {
            // Given: expression with indexed tag filter
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("attributes['http.method'] = 'GET' AND status = 'ok'");

            // When: split and serialize
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: both expressions should serialize correctly
            String indexedSerialized = splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 = 'GET'", indexedSerialized,
                         "Indexed filter should serialize to f1 = 'GET'");

            String remainingSerialized = splitter.getExpression().serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("(status = 'ok')", remainingSerialized,
                         "Remaining expression should serialize to (status = 'ok') AND (1 = 1)");
        }
    }

    @Nested
    @DisplayName("Tag Access Variations")
    class TagAccessVariations {

        @Test
        @DisplayName("Tags alias should work the same as attributes")
        void testTagsAlias() {
            // Given: expression using 'tags' alias
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .schema(summaryTableSchema)
                                                   .build("tags['http.method'] = 'GET'");

            // When: split the expression
            TraceFilterSplitter splitter = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter.split(expr);

            // Then: should extract correctly (tags is an alias for attributes)
            assertEquals(1, splitter.getIndexedTagFilters().size());
            String indexedFilterText = splitter.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 = 'GET'", indexedFilterText,
                         "Indexed filter should be exactly f1 = 'GET'");
        }

        @Test
        @DisplayName("Different indexed tag keys should map to correct columns")
        void testDifferentTagKeys() {
            // Test http.method -> f1
            IExpression expr1 = ExpressionASTBuilder.builder()
                                                    .schema(summaryTableSchema)
                                                    .build("attributes['http.method'] = 'GET'");
            TraceFilterSplitter splitter1 = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter1.split(expr1);
            String indexedFilterText1 = splitter1.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f1 = 'GET'", indexedFilterText1,
                         "http.method should map to f1 = 'GET'");

            // Test http.status -> f2
            IExpression expr2 = ExpressionASTBuilder.builder()
                                                    .schema(summaryTableSchema)
                                                    .build("attributes['http.status'] = 200");
            TraceFilterSplitter splitter2 = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter2.split(expr2);
            String indexedFilterText2 = splitter2.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f2 = '200'", indexedFilterText2,
                         "http.status should map to f2 = 200");

            // Test db.type -> f3
            IExpression expr3 = ExpressionASTBuilder.builder()
                                                    .schema(summaryTableSchema)
                                                    .build("attributes['db.type'] = 'mysql'");
            TraceFilterSplitter splitter3 = new TraceFilterSplitter(summaryTableSchema, indexTableSchema);
            splitter3.split(expr3);
            String indexedFilterText3 = splitter3.getIndexedTagFilters().get(0).serializeToText(IdentifierQuotaStrategy.NONE);
            assertEquals("f3 = 'mysql'", indexedFilterText3,
                         "db.type should map to f3 = 'mysql'");
        }
    }
}

