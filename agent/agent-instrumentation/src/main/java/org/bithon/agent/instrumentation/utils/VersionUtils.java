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

package org.bithon.agent.instrumentation.utils;

/**
 * @author frank.chen021@outlook.com
 * @date 7/1/25 11:55 am
 */
public class VersionUtils {
    /**
     * compare the left to the right
     *
     * @return -1 if the left < right;
     * 0 if the left == right;
     * 1 if the left > right
     */
    public static int compare(String left, String right) {
        String[] actualParts = left.split("\\.");
        String[] expectedParts = right.split("\\.");

        int length = Math.max(actualParts.length, expectedParts.length);
        for (int i = 0; i < length; i++) {
            int actualPart = i < actualParts.length ? Integer.parseInt(actualParts[i]) : 0;
            int expectedPart = i < expectedParts.length ? Integer.parseInt(expectedParts[i]) : 0;
            if (actualPart < expectedPart) {
                return -1;
            }
            if (actualPart > expectedPart) {
                return 1;
            }
        }
        return 0;
    }
}

