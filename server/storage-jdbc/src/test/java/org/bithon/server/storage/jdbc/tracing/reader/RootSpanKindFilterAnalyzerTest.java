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

package org.bithon.server.storage.jdbc.tracing.reader;

import org.bithon.component.commons.expression.IExpression;
import org.bithon.component.commons.expression.serialization.IdentifierQuotaStrategy;
import org.bithon.server.datasource.expression.ExpressionASTBuilder;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test cases for SpanKindIsRootDetector
 *
 * @author frank.chen021@outlook.com
 */
@DisplayName("SpanKindIsRootDetector Tests")
public class RootSpanKindFilterAnalyzerTest {

    @Test
    @DisplayName("Null expression should return false")
    void testNullExpression() {
        TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(null);
        assertFalse(result.isRootSpan());
        Assertions.assertNull(result.getExpression());
    }

    @Nested
    @DisplayName("Single Root Span Kind Tests")
    class SingleRootSpanKindTests {

        @Test
        @DisplayName("kind = 'SERVER' should return true")
        void testServerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'SERVER'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "SERVER is a root span kind");
            assertEquals("kind = 'SERVER'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind = 'TIMER' should return true")
        void testTimerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'TIMER'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "TIMER is a root span kind");
            assertEquals("kind = 'TIMER'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind = 'CONSUMER' should return true")
        void testConsumerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'CONSUMER'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "CONSUMER is a root span kind");
            assertEquals("kind = 'CONSUMER'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }
    }

    @Nested
    @DisplayName("Non-Root Span Kind Tests")
    class NonRootSpanKindTests {

        @Test
        @DisplayName("kind = 'CLIENT' should return false")
        void testClientKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'CLIENT'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertFalse(result.isRootSpan(), "CLIENT is not a root span kind");
            assertEquals("kind = 'CLIENT'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind = 'INTERNAL' should return false")
        void testInternalKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'INTERNAL'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertFalse(result.isRootSpan(), "INTERNAL is not a root span kind");
            assertEquals("kind = 'INTERNAL'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind = 'PRODUCER' should return false")
        void testProducerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'PRODUCER'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertFalse(result.isRootSpan(), "PRODUCER is not a root span kind");
            assertEquals("kind = 'PRODUCER'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }
    }

    @Nested
    @DisplayName("IN Expression Tests")
    class InExpressionTests {

        @Test
        @DisplayName("kind IN ('SERVER', 'TIMER') should return true")
        void testInWithAllRootSpans() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER', 'TIMER')");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "All values in IN are root span kinds");
            assertEquals("kind in ('SERVER', 'TIMER')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind IN ('SERVER', 'TIMER', 'CONSUMER') should return true")
        void testInWithAllThreeRootSpans() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER', 'TIMER', 'CONSUMER')");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "All values in IN are root span kinds (all 3 root kinds)");
            assertEquals("true", result.getExpression().serializeToText());
        }

        @Test
        @DisplayName("kind IN ('SERVER', 'CLIENT') should return true")
        void testInWithMixedKinds() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER', 'CLIENT')");
            // Returns true because the filter matches root spans (SERVER), so we can use the summary table
            // The filter itself will ensure we only get matching root spans from the summary table
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "IN contains root span kind (SERVER), so can use summary table");
            assertEquals("kind in ('SERVER', 'CLIENT')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind IN ('CLIENT', 'INTERNAL') should return false")
        void testInWithNoRootSpans() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('CLIENT', 'INTERNAL')");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertFalse(result.isRootSpan(), "IN contains no root span kinds");
            assertEquals("kind in ('CLIENT', 'INTERNAL')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind IN ('SERVER') should return true")
        void testInWithSingleRootSpan() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER')");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "Single root span kind in IN should return true");
            assertEquals("kind = 'SERVER'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }
    }

    @Nested
    @DisplayName("Other Field Tests")
    class OtherFieldTests {

        @Test
        @DisplayName("Expression with other field should return false")
        void testOtherField() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("appName = 'myapp'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertFalse(result.isRootSpan(), "Expression without 'kind' field should return false");
            assertEquals("appName = 'myapp'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("Expression with other field and kind should handle correctly")
        void testMixedFields() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'SERVER' AND appName = 'myapp'");
            // Should return true because it contains kind = 'SERVER' which is a root span
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "Expression with kind = 'SERVER' should return true even with other fields");
            assertEquals("(kind = 'SERVER') AND (appName = 'myapp')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }
    }

    @Nested
    @DisplayName("Case Sensitivity Tests")
    class CaseSensitivityTests {

        @Test
        @DisplayName("kind = 'server' (lowercase) should return true")
        void testLowerCaseServer() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'server'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "SpanKind.isRootSpan handles case conversion, lowercase should work");
            assertEquals("kind = 'server'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("kind = 'Server' (mixed case) should return true")
        void testMixedCaseServer() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'Server'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "SpanKind.isRootSpan handles case conversion, mixed case should work");
            assertEquals("kind = 'Server'", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }
    }

    @Nested
    @DisplayName("Complex Expression Tests")
    class ComplexExpressionTests {

        @Test
        @DisplayName("Complex AND expression with root span kind should return true")
        void testComplexAndWithRootKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'SERVER' AND status = 'ok' AND appName = 'myapp'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "Complex AND with root span kind should return true");
            assertEquals("(kind = 'SERVER') AND (status = 'ok') AND (appName = 'myapp')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("Complex AND expression without root span kind should return false")
        void testComplexAndWithoutRootKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("status = 'ok' AND appName = 'myapp'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertFalse(result.isRootSpan(), "Complex AND without kind field should return false");
            assertEquals("(status = 'ok') AND (appName = 'myapp')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("Complex AND with kind = 'CLIENT' should return false")
        void testComplexAndWithNonRootKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'CLIENT' AND status = 'ok'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertFalse(result.isRootSpan(), "Complex AND with non-root kind should return false");
            assertEquals("(kind = 'CLIENT') AND (status = 'ok')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }

        @Test
        @DisplayName("Complex AND with kind IN containing root span should return true")
        void testComplexAndWithKindIn() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER', 'TIMER') AND status = 'ok'");
            TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
            assertTrue(result.isRootSpan(), "Complex AND with kind IN containing root spans should return true");
            assertEquals("(kind in ('SERVER', 'TIMER')) AND (status = 'ok')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
        }
    }

    @Nested
    @DisplayName("Optimize in filter")
    class EdgeCaseTests {

        @Test
        void testOptimizedPlaceholderExpression() {
            {
                IExpression expr = ExpressionASTBuilder.builder()
                                                       .build("kind in ('SERVER', 'TIMER', 'CONSUMER')");
                TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
                assertTrue(result.isRootSpan());
                assertEquals("true", result.getExpression().serializeToText());
            }
            {
                IExpression expr = ExpressionASTBuilder.builder()
                                                       .build("kind in ('SERVER', 'TIMER')");
                TraceJdbcReader.AnalyzeResult result = TraceJdbcReader.RootSpanKindFilterAnalyzer.analyze(expr);
                assertTrue(result.isRootSpan());
                assertEquals("kind in ('SERVER', 'TIMER')", result.getExpression().serializeToText(IdentifierQuotaStrategy.NONE));
            }
        }
    }
}

