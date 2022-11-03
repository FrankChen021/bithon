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
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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

    /**
     * Support simple format
     */
    @JsonCreator
    public static Limit fromString(String limit) {
        return new Limit(Integer.parseInt(limit), 0);
    }

    /**
     * Support simple format
     */
    @JsonCreator
    public static Limit fromNumber(Number limit) {
        return new Limit(limit.intValue(), 0);
    }
}
