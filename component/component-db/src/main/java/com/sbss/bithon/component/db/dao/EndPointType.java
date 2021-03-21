package com.sbss.bithon.component.db.dao;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 9:28 下午
 */
public enum EndPointType {

    UNKNOWN(-1),
    APPLICATION(0),

    // Database
    DB_UNKOWN(1),
    DB_H2(1),
    DB_MYSQL(1),
    DB_MONGO(3),

    REDIS(2),
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
