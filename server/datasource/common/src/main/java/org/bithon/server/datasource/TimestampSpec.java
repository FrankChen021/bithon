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

package org.bithon.server.datasource;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;


public class TimestampSpec {
    public static final String COLUMN_ALIAS = "_timestamp";

    public static final String DEFAULT_COLUMN = "timestamp";
    private final String timestampColumn;

    @JsonCreator
    public TimestampSpec(@JsonProperty("column") @Nullable String timestampColumn) {
        this.timestampColumn = (timestampColumn == null) ? DEFAULT_COLUMN : timestampColumn;
    }

    @JsonProperty("column")
    public String getColumnName() {
        return timestampColumn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimestampSpec that = (TimestampSpec) o;
        return timestampColumn.equals(that.timestampColumn);
    }

    @Override
    public int hashCode() {
        return timestampColumn.hashCode();
    }

    @Override
    public String toString() {
        return "TimestampSpec{" + "timestampColumn='" + timestampColumn + '\'' + '}';
    }
}
