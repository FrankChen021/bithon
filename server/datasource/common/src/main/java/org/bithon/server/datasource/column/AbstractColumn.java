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

package org.bithon.server.datasource.column;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;


/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/11 10:11 上午
 */
@Data
public abstract class AbstractColumn implements IColumn {

    /**
     * this is the name used at the underlying storage
     */
    @Getter
    @NotNull
    private final String name;

    /**
     * This is the name that is used at business layer
     * If it's not specified, it's default to {@link #name}.
     * <p>
     * Some underlying storage uses some meaningless name, and the business layer maps a business name to that meaningless name.
     */
    @Getter
    private final String alias;

    public AbstractColumn(@NotNull String name,
                          @Nullable String alias) {
        this.name = name;
        this.alias = alias == null ? name : alias;
    }
}
