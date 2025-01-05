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

package org.bithon.server.storage.alerting;

import lombok.Getter;
import org.bithon.component.commons.utils.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * When an alert rule is based on GROUP-BY clause, this class holds the labels of a series
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/29 14:11
 */
public class Labels {
    private final List<String> values = new ArrayList<>();

    @Getter
    private String id = "";

    public boolean isEmpty() {
        return values.isEmpty();
    }

    public void add(String label, String value) {
        values.add(value);
        if (!id.isEmpty()) {
            id += ", ";
        }
        id += StringUtils.format("%s = '%s'", label, value);
    }

    public List<String> getValues() {
        return values;
    }

    @Override
    public String toString() {
        return id;
    }
}
