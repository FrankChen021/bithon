package com.sbss.bithon.server.meta;

import com.sbss.bithon.component.db.dao.EndPointType;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/6 12:05 下午
 */
@Getter
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
public class EndPointLink {
    private EndPointType srcEndPointType;
    private String srcEndpoint;
    private EndPointType dstEndpointType;
    private String dstEndpoint;
}
