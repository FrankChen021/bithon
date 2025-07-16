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

package org.bithon.server.web.service.diagnosis;


import org.bithon.component.commons.utils.StringUtils;

/**
 * @author frank.chen021@outlook.com
 * @date 13/7/25 11:15 am
 */
public class StackFrame {
    /**
     * See type defined {@link one.convert.Frame}
     */
    public byte type;
    public String typeName;
    public String method;
    public int modifiers;
    public String[] parameters;
    public String returnType;
    public int location;

    @Override
    public String toString() {
        return StringUtils.format("%s.%s(%s) %s",
                                  typeName == null ? "" : typeName,
                                  method == null ? "" : method,
                                  parameters == null ? "" : String.join(", ", parameters),
                                  location > 0 ? StringUtils.format("line: %d", location) : "");
    }
}
