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
