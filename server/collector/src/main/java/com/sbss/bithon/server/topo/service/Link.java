package com.sbss.bithon.server.topo.service;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:46
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Link {
    private String srcEndpoint;
    private String dstEndpoint;

    private double minResponseTime;
    private double maxResponseTime;
    private double avgResponseTime;
    private long callCount;
}
