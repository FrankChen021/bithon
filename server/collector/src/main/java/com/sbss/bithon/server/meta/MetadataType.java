package com.sbss.bithon.server.meta;

import lombok.Getter;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/1/11 10:59 下午
 */
public enum MetadataType {
    APPLICATION("application"),
    APP_INSTANCE("app-instance"),
    HTTP_TARGET_HOST("http-target-host"),
    HTTP_URI("http-uri"),
    DB_INSTANCE("db-instance"),
    DB_DATABASE("db-database");

    @Getter
    private final String type;

    MetadataType(String type) {
        this.type = type;
    }
}
