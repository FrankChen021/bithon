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

package org.bithon.server.commons.autocomplete;

import java.util.Objects;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/4/13 17:38
 */
public class TokenHint {
    public final int parserRuleIndex;
    public final int expectedTokenType;

    public TokenHint(int parserRuleIndex, int expectedTokenType) {
        this.parserRuleIndex = parserRuleIndex;
        this.expectedTokenType = expectedTokenType;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        TokenHint that = (TokenHint) o;
        return parserRuleIndex == that.parserRuleIndex && expectedTokenType == that.expectedTokenType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(parserRuleIndex, expectedTokenType);
    }
}
