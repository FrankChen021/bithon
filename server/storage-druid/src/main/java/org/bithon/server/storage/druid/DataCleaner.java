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

package org.bithon.server.storage.druid;

import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;

/**
 * @author frank.chen021@outlook.com
 * @date 28/7/22 1:15 PM
 */
@Slf4j
public class DataCleaner {

    private final DruidConfig config;
    private final DSLContext dsl;

    public DataCleaner(DruidConfig config, DSLContext dsl) {
        this.config = config;
        this.dsl = dsl;
    }

    public void clean(String table, String timestamp) {
        log.info("\tDrop [{}] on [{}]", table, timestamp);
    }
}
