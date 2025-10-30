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
    @DisplayName("Null expression should return true")
    void testNullExpression() {
        Assertions.assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(null));
    }

    @Nested
    @DisplayName("Single Root Span Kind Tests")
    class SingleRootSpanKindTests {

        @Test
        @DisplayName("kind = 'SERVER' should return true")
        void testServerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'SERVER'");
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "SERVER is a root span kind");
        }

        @Test
        @DisplayName("kind = 'TIMER' should return true")
        void testTimerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'TIMER'");
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "TIMER is a root span kind");
        }

        @Test
        @DisplayName("kind = 'CONSUMER' should return true")
        void testConsumerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'CONSUMER'");
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "CONSUMER is a root span kind");
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
            assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "CLIENT is not a root span kind");
        }

        @Test
        @DisplayName("kind = 'INTERNAL' should return false")
        void testInternalKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'INTERNAL'");
            assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "INTERNAL is not a root span kind");
        }

        @Test
        @DisplayName("kind = 'PRODUCER' should return false")
        void testProducerKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'PRODUCER'");
            assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "PRODUCER is not a root span kind");
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
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "All values in IN are root span kinds");
        }

        @Test
        @DisplayName("kind IN ('SERVER', 'TIMER', 'CONSUMER') should return true")
        void testInWithAllThreeRootSpans() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER', 'TIMER', 'CONSUMER')");
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "All values in IN are root span kinds (all 3 root kinds)");
        }

        @Test
        @DisplayName("kind IN ('SERVER', 'CLIENT') should return true")
        void testInWithMixedKinds() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER', 'CLIENT')");
            // Returns true because the filter matches root spans (SERVER), so we can use the summary table
            // The filter itself will ensure we only get matching root spans from the summary table
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "IN contains root span kind (SERVER), so can use summary table");
        }

        @Test
        @DisplayName("kind IN ('CLIENT', 'INTERNAL') should return false")
        void testInWithNoRootSpans() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('CLIENT', 'INTERNAL')");
            assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "IN contains no root span kinds");
        }

        @Test
        @DisplayName("kind IN ('SERVER') should return true")
        void testInWithSingleRootSpan() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER')");
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "Single root span kind in IN should return true");
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
            assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "Expression without 'kind' field should return false");
        }

        @Test
        @DisplayName("Expression with other field and kind should handle correctly")
        void testMixedFields() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'SERVER' AND appName = 'myapp'");
            // Should return true because it contains kind = 'SERVER' which is a root span
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "Expression with kind = 'SERVER' should return true even with other fields");
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
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "SpanKind.isRootSpan handles case conversion, lowercase should work");
        }

        @Test
        @DisplayName("kind = 'Server' (mixed case) should return true")
        void testMixedCaseServer() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'Server'");
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "SpanKind.isRootSpan handles case conversion, mixed case should work");
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
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "Complex AND with root span kind should return true");
        }

        @Test
        @DisplayName("Complex AND expression without root span kind should return false")
        void testComplexAndWithoutRootKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("status = 'ok' AND appName = 'myapp'");
            assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "Complex AND without kind field should return false");
        }

        @Test
        @DisplayName("Complex AND with kind = 'CLIENT' should return false")
        void testComplexAndWithNonRootKind() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind = 'CLIENT' AND status = 'ok'");
            assertFalse(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "Complex AND with non-root kind should return false");
        }

        @Test
        @DisplayName("Complex AND with kind IN containing root span should return true")
        void testComplexAndWithKindIn() {
            IExpression expr = ExpressionASTBuilder.builder()
                                                   .build("kind IN ('SERVER', 'TIMER') AND status = 'ok'");
            assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr), "Complex AND with kind IN containing root spans should return true");
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
                assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr));
                assertEquals("1 in (1)", expr.serializeToText());
            }
            {
                IExpression expr = ExpressionASTBuilder.builder()
                                                       .build("kind in ('SERVER', 'TIMER')");
                assertTrue(TraceJdbcReader.RootSpanKindFilterAnalyzer.isOnRootSpanOnly(expr));
                assertEquals("kind in ('SERVER', 'TIMER')", expr.serializeToText(IdentifierQuotaStrategy.NONE));
            }
        }
    }
}

