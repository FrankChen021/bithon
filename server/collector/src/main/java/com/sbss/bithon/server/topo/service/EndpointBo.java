package com.sbss.bithon.server.topo.service;

import com.sbss.bithon.component.db.dao.EndPointType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:45
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EndpointBo {
    private EndPointType type;
    private String name;
    private long x;
    private long y;
}
