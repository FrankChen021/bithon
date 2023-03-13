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

package org.bithon.server.storage.meta;

import lombok.Data;

import java.util.Objects;

/**
 * @author Frank Chen
 * @date 27/2/23 4:50 pm
 */
@Data
public class Instance implements Comparable<Instance> {
    private final String appName;
    private final String appType;
    private final String instanceName;

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Instance instance = (Instance) o;
        return Objects.equals(appName, instance.appName) && Objects.equals(appType, instance.appType) && Objects.equals(instanceName,
                                                                                                                        instance.instanceName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(appName, appType, instanceName);
    }

    @Override
    public int compareTo(Instance o) {
        int v = this.appName.compareTo(o.appName);
        if (v != 0) {
            return v;
        }

        v = this.appType.compareTo(o.appType);
        if (v != 0) {
            return v;
        }

        return this.instanceName.compareTo(o.instanceName);
    }
}
