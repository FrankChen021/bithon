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

import org.junit.Test;

/**
 * @author frank.chen021@outlook.com
 * @date 15/4/24 2:52 pm
 */
public class AutoSuggesterTest {

    /**
     * r1: c 'A';
     * r2: c 'B';
     * c: 'F'
     * <p>
     * Given input 'F', and starting from rule r1, there should be only one suggestion 'A'.
     */
    @Test
    public void suggest_withSingleTokenComingUp_shouldSuggestSingleToken() {
        new AutoSuggesterVerificationBuilder()
            .givenGrammar("r1: c 'A'; r2:  c 'B';  c: 'F'")
            .whenInput("F")
            .thenExpect("A");
    }
}

