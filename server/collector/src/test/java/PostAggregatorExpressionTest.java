import com.sbss.bithon.server.metric.aggregator.IMetricSpec;
import com.sbss.bithon.server.metric.aggregator.PostAggregatorExpressionVisitor;
import com.sbss.bithon.server.metric.aggregator.PostAggregatorMetricSpec;
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
