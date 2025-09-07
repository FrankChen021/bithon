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

package org.bithon.agent.plugins.test;

/**
 * Placeholder class to ensure this module has content for JAR creation.
 * This module is primarily a test-only module, but Maven requires some main content
 * to avoid "empty JAR" warnings and installation issues.
 * <p>
 * This class serves no functional purpose other than satisfying Maven's requirements.
 */
public final class TestModulePlaceholder {

    /**
     * Private constructor to prevent instantiation.
     * This is a utility class that should never be instantiated.
     */
    private TestModulePlaceholder() {
        throw new UnsupportedOperationException("This is a placeholder class and should not be instantiated");
    }

    /**
     * Returns the module name for identification purposes.
     *
     * @return the name of this test module
     */
    public static String getModuleName() {
        return "agent-plugins-test";
    }
}
