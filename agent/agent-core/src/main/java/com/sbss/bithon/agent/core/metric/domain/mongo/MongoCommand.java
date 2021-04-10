/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.agent.core.metric.domain.mongo;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/28 11:43
 */
public class MongoCommand {
    private final String database;
    private final String collection;
    private final String command;

    public String getDatabase() {
        return database;
    }

    public String getCollection() {
        return collection;
    }

    public String getCommand() {
        return command;
    }

    public MongoCommand(String database, String collection, String command) {
        this.database = database;
        this.collection = collection;
        this.command = command;
    }
}
