/*
 *    Copyright 2020 bithon.org
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package org.bithon.server.storage.meta;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/4 9:28 下午
 */
public enum EndPointType {

    USER(-1),
    APPLICATION(0),

    // Database
    DB_UNKNOWN(10),
    DB_H2(11),
    DB_MYSQL(12),
    DB_MONGO(13),
    DB_CLICKHOUSE(14),

    REDIS(20),
    WEB_SERVICE(30),

    ZOOKEEPER(40),

    GRPC(50);

    public int getType() {
        return type;
    }

    private final int type;

    EndPointType(int type) {
        this.type = type;
    }
}
