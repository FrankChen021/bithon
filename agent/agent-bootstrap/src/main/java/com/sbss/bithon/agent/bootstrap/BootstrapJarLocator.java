package com.sbss.bithon.agent.bootstrap;

import com.sbss.bithon.agent.boot.expt.AgentException;

import java.io.File;
import java.net.URL;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/4/3 00:22
 */
class BootstrapJarLocator {
    /**
     * @return which jar the given class is in
     */
    public File locate(String className) {
        final String internalClassName = className.replace('.', '/') + ".class";
        final URL classURL = ClassLoader.getSystemResource(internalClassName);
        if (classURL == null) {
            throw new AgentException("Unable to locate class [%s]", className);
        }

        if (!"jar".equals(classURL.getProtocol())) {
            throw new AgentException("Unknown agent-bootstrap.jar location: %s", classURL.toString());
        }

        /**
         * classURL.getPath returns a path as below
         * file:/directory/dest/agent-bootstrap.jar!/com/sbss/bithon/agent/bootstrap/AgentApp.class
         *
         * and we extract agent jar file path from it
         */
        String path = classURL.getPath();
        int jarIndex = path.indexOf("!/");
        if (jarIndex == -1) {
            throw new AgentException("Invalid path [%s]" + path);
        }
        return new File(path.substring("file:".length(), jarIndex));
    }
}
