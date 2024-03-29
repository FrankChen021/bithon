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

package org.bithon.server.commons.matcher;

import lombok.Getter;

/**
 * @author Frank Chen
 * @date 17/8/23 2:04 pm
 */
public class NotMatcher implements IMatcher {
    @Getter
    private final IMatcher matcher;

    public NotMatcher(IMatcher matcher) {
        this.matcher = matcher;
    }

    @Override
    public boolean matches(Object input) {
        return false;
    }

    @Override
    public <T> T accept(IMatcherVisitor<T> visitor) {
        return visitor.visit(this);
    }
}
