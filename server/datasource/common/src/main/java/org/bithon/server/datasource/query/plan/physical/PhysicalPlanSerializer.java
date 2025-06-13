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

package org.bithon.server.datasource.query.plan.physical;


/**
 * @author frank.chen021@outlook.com
 * @date 11/6/25 4:20 pm
 */
public class PhysicalPlanSerializer {
    private final StringBuilder builder = new StringBuilder(128);

    public PhysicalPlanSerializer append(String text) {
        builder.append(text);
        return this;
    }

    public PhysicalPlanSerializer append(String indent, String text) {
        for (String line : text.split("\\r?\\n")) {
            builder.append(indent).append(line).append("\n");
        }
        return this;
    }

    public String getSerializedPlan() {
        return builder.toString();
    }

    public PhysicalPlanSerializer append(char c) {
        builder.append(c);
        return this;
    }
}
