package com.sbss.bithon.component.db.dao;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 9:28 下午
 */
public enum EndPointType {

    UNKNOWN(-1),
    APPLICATION(0),
    MYSQL(1),
    REDIS(2),
    MONGO(3),
    DOMAIN(4),
    WEB_SERVICE(5);

    public int getType() {
        return type;
    }

    private final int type;

    EndPointType(int type) {
        this.type = type;
    }
}
