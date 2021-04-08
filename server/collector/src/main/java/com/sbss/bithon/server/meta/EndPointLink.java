package com.sbss.bithon.server.meta;

import com.sbss.bithon.component.db.dao.EndPointType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/6 12:05 下午
 */
@Builder
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EndPointLink {
    private long timestamp;

    // dimension
    private EndPointType srcEndpointType;
    private String srcEndpoint;
    private EndPointType dstEndpointType;
    private String dstEndpoint;

    // metric
    private long interval;
    private long callCount;
    private long errorCount;
    private long responseTime;
    private long minResponseTime;
    private long maxResponseTime;

    public long getTimestamp() {
        return timestamp;
    }

    public List<Object> getDimensions() {
        return Arrays.asList(srcEndpointType, srcEndpoint, dstEndpointType, dstEndpoint);
    }

    public Map<String, Number> getMetrics() {
        return null;
    }
}
