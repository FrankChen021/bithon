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

package org.bithon.server.storage.datasource.filter;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.bithon.server.storage.datasource.input.IInputRow;

/**
 * @author frankchen
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes(value = {
    //@JsonSubTypes.Type(name = "endwith", value = EndwithFilter.class),
    @JsonSubTypes.Type(name = "==", value = EqualFilter.class),
    //@JsonSubTypes.Type(name = ">", value = GreaterThanFilter.class),
    //@JsonSubTypes.Type(name = "or", value = OrFilter.class)
})
public interface IFilter {

    boolean shouldInclude(IInputRow inputRow);
}
