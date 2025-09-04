package org.bithon.agent.plugins.test;

/**
 * Placeholder class to ensure this module has content for JAR creation.
 * This module is primarily a test-only module, but Maven requires some main content
 * to avoid "empty JAR" warnings and installation issues.
 * 
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
