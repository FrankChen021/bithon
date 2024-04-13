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

import java.util.Objects;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/13 17:38
 */
public class GrammarRule {
    int ruleIndex;
    int nextTokenType;

    public GrammarRule(int ruleIndex, int nextTokenType) {
        this.ruleIndex = ruleIndex;
        this.nextTokenType = nextTokenType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GrammarRule that = (GrammarRule) o;
        return ruleIndex == that.ruleIndex && nextTokenType == that.nextTokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(ruleIndex, nextTokenType);
    }
}
