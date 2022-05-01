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

package org.bithon.server.alerting.common.algorithm;

/**
 * @author frankchen
 * @date 2020-03-27 11:06:09
 */
public enum SmoothAlgorithm {

    NONE("none"), MovingAverage("mv");

    private final String name;

    SmoothAlgorithm(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

}
