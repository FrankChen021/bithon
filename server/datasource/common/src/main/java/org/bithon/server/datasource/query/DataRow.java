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

package org.bithon.server.datasource.query;


import lombok.Getter;

import java.util.List;

/**
 * @author frank.chen021@outlook.com
 * @date 28/10/25 4:10 pm
 */
@Getter
public class DataRow {

    /**
     * see {@link DataRowType}
     */
    private final String type;

    /**
     * Either:
     * 1. a Map<String, Object> for {@link DataRowType#PROGRESS} type
     * 2. List<ColumnMetadata> for {@link DataRowType#META} type
     * 3. Map<String, Object>/Object[] for {@link DataRowType#DATA} type determined by the given {@link ResultFormat}
     */
    private final Object payload;

    private DataRow(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    public static DataRow data(Object payload) {
        return new DataRow(DataRowType.DATA, payload);
    }

    public static DataRow meta(List<ColumnMetadata> meta) {
        return new DataRow(DataRowType.META, meta);
    }

    public static DataRow progress(Object payload) {
        return new DataRow(DataRowType.PROGRESS, payload);
    }
}
