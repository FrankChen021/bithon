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

package org.bithon.agent.sentinel;

/**
 * @author frankchen
 */
public interface IUrlMatcher {

    static IUrlMatcher createMatcher(String pattern) {
        if ("*".equals(pattern)) {
            return new MatchAllMatcher();
        }

        int starIndex = pattern.indexOf('*');
        if (starIndex == pattern.length() - 1 && pattern.charAt(pattern.length() - 2) == '/') {
            // there's only one '*' and it's at the end of the pattern
            return new EndingStarMatcher(pattern);
        }

        if (starIndex >= 0) {
            return new NormalizedMatcher(pattern);
        } else {
            return new ExactMatch(pattern);
        }
    }

    String getPattern();

    boolean matches(String input);

    class NormalizedMatcher implements IUrlMatcher {
        private final String[] patternParts;
        private final String pattern;

        public NormalizedMatcher(String pattern) {
            this.patternParts = pattern.split("/");
            this.pattern = pattern;
        }

        @Override
        public String getPattern() {
            return pattern;
        }

        @Override
        public boolean matches(String input) {
            if (input == null) {
                return false;
            }

            if (input.length() == 0) {
                return false;
            }

            int i = 0;
            String[] parts = input.split("/");
            for (int j = 0; i < patternParts.length && j < parts.length; i++, j++) {
                if ("*".equals(patternParts[i])) {
                    continue;
                }
                if (!patternParts[i].equals(parts[j])) {
                    return false;
                }
            }

            return i == patternParts.length;
        }
    }

    class EndingStarMatcher implements IUrlMatcher {
        private final String pattern;

        public EndingStarMatcher(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String input) {
            // compare input with pattern without ending /*
            if (pattern.regionMatches(0, input, 0, pattern.length() - 2)) {
                if (input.length() == (pattern.length() - 2)) {
                    return true;
                } else if ('/' == input.charAt(pattern.length() - 2)) {
                    return true;
                }
            }
            return false;
        }

        @Override
        public String getPattern() {
            return pattern;
        }
    }

    class MatchAllMatcher implements IUrlMatcher {
        @Override
        public boolean matches(String input) {
            return true;
        }

        @Override
        public String getPattern() {
            return "*";
        }
    }

    class ExactMatch implements IUrlMatcher {
        private final String pattern;

        public ExactMatch(String pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean matches(String input) {
            return pattern.equals(input);
        }

        @Override
        public String getPattern() {
            return pattern;
        }
    }
}
