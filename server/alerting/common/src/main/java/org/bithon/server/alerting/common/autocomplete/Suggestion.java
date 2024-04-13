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

package org.bithon.server.alerting.common.autocomplete;

import lombok.Getter;
import lombok.Setter;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/13 18:00
 */
@Getter
public class Suggestion implements Comparable<Suggestion> {
    /**
     * Suggested text
     */
    private final String text;

    private final int tokenType;

    @Getter
    @Setter
    private Object tag;

    public Suggestion(int tokenType, String text) {
        this.tokenType = tokenType;
        this.text = text;
    }

    @Override
    public String toString() {
        return text;
    }

    @Override
    public int compareTo(Suggestion o) {
        if (this.tokenType == o.tokenType) {
            return this.text.compareTo(o.text);
        }
        return this.tokenType - o.tokenType;
    }
}
