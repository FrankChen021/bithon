/*
 *    Copyright 2020 bithon.cn
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

import com.sbss.bithon.server.metric.aggregator.spec.IMetricSpec;
import com.sbss.bithon.server.metric.aggregator.spec.PostAggregatorExpressionVisitor;
import com.sbss.bithon.server.metric.aggregator.spec.PostAggregatorMetricSpec;
import org.junit.Assert;
import org.junit.Test;

public class PostAggregatorExpressionTest {

    @Test
    public void testIntervalExpression() {
        PostAggregatorMetricSpec metricSpec = new PostAggregatorMetricSpec("avg",
                                                                           "dis",
                                                                           "",
                                                                           "1000/interval",
                                                                           "long",
                                                                           true);
        final StringBuilder sb = new StringBuilder();
        metricSpec.visitExpression(new PostAggregatorExpressionVisitor() {
            @Override
            public void visitMetric(IMetricSpec metricSpec) {
            }

            @Override
            public void visitNumber(String number) {
                sb.append(number);
            }

            @Override
            public void visitorOperator(String operator) {
                sb.append(operator);
            }

            @Override
            public void startBrace() {
            }

            @Override
            public void endBrace() {
            }

            @Override
            public void visitVariable(String variable) {
                sb.append(10);
            }
        });
        Assert.assertEquals("1000/10", sb.toString());
    }
}
