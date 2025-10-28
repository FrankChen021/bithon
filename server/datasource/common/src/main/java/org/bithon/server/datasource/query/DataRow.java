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
public class DataRow<T> {

    /**
     * see {@link DataRowType}
     */
    private final String type;
    private final T payload;

    private DataRow(String type, T payload) {
        this.type = type;
        this.payload = payload;
    }

    public static <T> DataRow<T> data(T payload) {
        return new DataRow<>(DataRowType.DATA, payload);
    }

    public static <T> DataRow<T> progress(T payload) {
        return new DataRow<>(DataRowType.PROGRESS, payload);
    }

    public static class Meta extends DataRow<List<ColumnMetadata>> {
        public Meta(List<ColumnMetadata> payload) {
            super(DataRowType.META, payload);
        }

        public static Meta of(List<ColumnMetadata> columnMetadata) {
            return new Meta(columnMetadata);
        }
    }
}
