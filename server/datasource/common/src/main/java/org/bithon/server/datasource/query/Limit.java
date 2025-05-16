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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bithon.component.commons.utils.Preconditions;


/**
 * @author frank.chen021@outlook.com
 * @date 17/4/22 5:23 PM
 */
@Data
@AllArgsConstructor
public class Limit {
    private final int limit;
    private final int offset;

    @JsonCreator
    public Limit(@Nullable @JsonProperty("limit") Integer limit,
                 @Nullable @JsonProperty("offset") Integer offset) {
        this.limit = limit == null ? 10 : limit;
        this.offset = offset == null ? 0 : offset;

        Preconditions.checkIfTrue(this.limit > 0 && this.limit <= 65536, "limit must be in the range of [1, 65536]");
        Preconditions.checkIfTrue(this.offset >= 0, "offset must be >= 0");
    }

    /**
     * Support simple format
     * Only string is supported when there's also an object format
     */
    @JsonCreator
    public static Limit fromString(String limit) {
        try {
            return new Limit(Integer.parseInt(limit), 0);
        } catch (NumberFormatException ignored) {
            throw new Preconditions.InvalidValueException("Given limit [%s] is not a valid number", limit);
        }
    }

    @JsonCreator
    public static Limit fromLong(long limit) {
        return new Limit((int) limit, 0);
    }
}
