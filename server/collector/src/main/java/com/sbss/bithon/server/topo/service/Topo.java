package com.sbss.bithon.server.topo.service;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/20 21:45
 */
@Data
public class Topo {
    private Set<EndpointBo> endpoints = new HashSet<>();
    private List<Link> links = new ArrayList<>();

    public EndpointBo getEndpointByName(String dstEndpoint) {
        return null;
    }

    public void addEndpoint(EndpointBo endpoint) {
        this.endpoints.add(endpoint);
    }

    public void addLink(Link link) {
        this.links.add(link);
    }
}
