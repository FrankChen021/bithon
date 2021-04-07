package com.sbss.bithon.server.webapp.rpc.metric;

import org.springframework.web.bind.annotation.PostMapping;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/15 12:15 上午
 */
public interface IMetricRpc {

    @PostMapping("/api/metric/")
    void a();
}
