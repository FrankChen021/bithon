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

package org.bithon.server.storage.datasource;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bithon.server.storage.datasource.typing.IDataType;

/**
 * @author Frank Chen
 * @date 29/10/22 10:47 pm
 */
public interface IColumnSpec {

    /**
     * the name in the storage.
     * can NOT be null
     */
    String getName();

    @JsonIgnore
    IDataType getDataType();
}
