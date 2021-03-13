package com.sbss.bithon.server.metric.metric.aggregator;

import com.sbss.bithon.server.metric.DataSourceSchema;
import com.sbss.bithon.server.metric.metric.IMetricSpec;
import com.sbss.bithon.server.metric.metric.IMetricSpecVisitor;
import com.sbss.bithon.server.metric.typing.IValueType;

/**
 *
 * @author frankchen
 */
public class PostAggregatorMetricSpec implements IMetricSpec {
    @Override
    public String getType() {
        return "postAggregator";
    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public boolean isVisible() {
        return false;
    }

    @Override
    public String getDisplayText() {
        return null;
    }

    @Override
    public String getUnit() {
        return null;
    }

    @Override
    public IValueType getValueType() {
        return null;
    }

    @Override
    public void setOwner(DataSourceSchema dataSource) {

    }

    @Override
    public String validate(Object input) {
        return null;
    }

    @Override
    public <T> T accept(IMetricSpecVisitor<T> visitor) {
        return null;
    }
}
