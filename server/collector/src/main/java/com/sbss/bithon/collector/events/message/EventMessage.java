package com.sbss.bithon.collector.events.message;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/2/14 4:11 下午
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventMessage {
    private String appName;
    private String instanceName;
    private Long timestamp;
    private String type;
    private Map<String, String> args;
}
