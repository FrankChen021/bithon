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

package org.bithon.server.storage.datasource.query;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.annotation.Nullable;

/**
 * @author frank.chen021@outlook.com
 * @date 17/4/22 5:23 PM
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Limit {
    private int limit = 10;
    private int offset = 0;

    @JsonCreator
    public Limit(@JsonProperty("limit") Integer limit,
                 @Nullable @JsonProperty("offset") Integer offset) {
        this.limit = limit;
        this.offset = offset == null ? 0 : offset;
    }

    /**
     * Support simple format
     * Only string is supported when there's also an object format
     */
    @JsonCreator
    public static Limit fromString(String limit) {
        return new Limit(Integer.parseInt(limit), 0);
    }
}
