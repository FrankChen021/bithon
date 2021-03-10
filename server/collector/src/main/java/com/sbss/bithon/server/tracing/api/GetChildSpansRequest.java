package com.sbss.bithon.server.tracing.api;

import lombok.Data;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/6 4:35 下午
 */
@Data
public class GetChildSpansRequest {
    private String parentSpanId;
}
