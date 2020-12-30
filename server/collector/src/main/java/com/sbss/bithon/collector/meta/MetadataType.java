package com.sbss.bithon.collector.meta;

import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:59 下午
 */
public enum MetadataType {
    APPLICATION("application"),
    INSTANCE("instance"),
    URI("uri");

    @Getter
    private final String type;

    MetadataType(String type) {
        this.type = type;
    }
}
