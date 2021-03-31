package com.sbss.bithon.agent.bootstrap;

import com.sbss.bithon.agent.core.config.AgentConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.loader.PluginClassLoader;
import com.sbss.bithon.agent.core.plugin.loader.PluginInstaller;
import com.sbss.bithon.agent.core.setting.AgentSettingManager;
import shaded.org.apache.log4j.xml.DOMConfigurator;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static java.io.File.pathSeparator;
import static java.io.File.separator;

class AgentStarter {
    private static final Logger log = LoggerFactory.getLogger(AgentStarter.class);

    private static final String CONF_LOG_FILE = "log4j.xml";
    private static final String CLASS_PATH = System.getProperty("java.class.path");
    private static final String CATALINA_HOME = System.getProperty("catalina.home");
    private final String agentPath;

    AgentStarter() {
        agentPath = new File(AgentStarter.class.getProtectionDomain()
                                               .getCodeSource()
                                               .getLocation()
                                               .getFile()).getParentFile().getPath();
    }

    void start(Instrumentation inst) throws Exception {
        showBanner();

        // init log
        DOMConfigurator.configure(agentPath + separator + AgentContext.CONF_DIR + separator + CONF_LOG_FILE);

        AgentContext agentContext = AgentContext.createInstance(agentPath);

        ensureTemporaryDir(agentContext.getConfig());

        // init setting
        AgentSettingManager.createInstance(agentContext.getAppInstance(),
                                           agentContext.getConfig().getFetcher());

        loadContext(agentContext.getConfig());

        PluginClassLoader.createInstance();

        PluginInstaller.install(agentContext, inst);
    }

    /**
     * The banner is generated on https://manytools.org/hacker-tools/ascii-banner/ with font = 3D-ASCII
     */
    private void showBanner() {
        System.out.println(" ________  ___  _________  ___  ___  ________  ________      \n"
                           + "|\\   __  \\|\\  \\|\\___   ___\\\\  \\|\\  \\|\\   __  \\|\\   ___  \\    \n"
                           + "\\ \\  \\|\\ /\\ \\  \\|___ \\  \\_\\ \\  \\\\\\  \\ \\  \\|\\  \\ \\  \\\\ \\  \\   \n"
                           + " \\ \\   __  \\ \\  \\   \\ \\  \\ \\ \\   __  \\ \\  \\\\\\  \\ \\  \\\\ \\  \\  \n"
                           + "  \\ \\  \\|\\  \\ \\  \\   \\ \\  \\ \\ \\  \\ \\  \\ \\  \\\\\\  \\ \\  \\\\ \\  \\ \n"
                           + "   \\ \\_______\\ \\__\\   \\ \\__\\ \\ \\__\\ \\__\\ \\_______\\ \\__\\\\ \\__\\\n"
                           + "    \\|_______|\\|__|    \\|__|  \\|__|\\|__|\\|_______|\\|__| \\|__|\n"
                           + "                                                             ");
    }

    private void loadContext(AgentConfig config) throws Exception {
        // springBoot project
        if (null == CATALINA_HOME || CATALINA_HOME.trim().length() <= 0) {
            logSeparate(config);
        }
    }

    private void ensureTemporaryDir(AgentConfig config) {
        File tmpDir = new File(agentPath + separator + AgentContext.TMP_DIR + separator +
                        config.getBootstrap().getAppName());

        if (!tmpDir.exists()) {
            tmpDir.mkdirs();
        }
    }

    private void logSeparate(AgentConfig config) throws IOException {
        String targetJar = getTargetJar();
        if (new File(targetJar).isDirectory()) {
            return;
        }
        try (JarFile jarFile = new JarFile(targetJar)) {
            String classesDir = jarFile.getManifest().getMainAttributes().getValue("Spring-Boot-Classes");
            if (classesDir == null) {
                return;
            }

            Optional<JarEntry> classesEntry = jarFile.stream()
                                                     .filter(e -> e.getName().startsWith(classesDir) &&
                                                                  config.getBootstrap()
                                                                        .getAppLogs()
                                                                        .contains(e.getName()
                                                                                   .substring(e.getName()
                                                                                               .lastIndexOf("/") +
                                                                                              1)))
                                                     .findFirst();
            log.info("SpringBoot log config file name: " + classesEntry);
            classesEntry.ifPresent(e -> {
                String key = "logging.config";
                String fileName = e.getName().substring(e.getName().lastIndexOf("/") + 1);
                String value = String.format("classpath:%s", fileName);
                log.info(String.format("Set system properties [%s = %s]", key, value));
                System.setProperty(key, value);
            });
        }
    }

    private String getTargetJar() {
        String selfJar = new File(this.agentPath).getName();
        if (null != CLASS_PATH && CLASS_PATH.trim().length() > 0) {
            return CLASS_PATH.indexOf(pathSeparator) > 0 ? Arrays.stream(CLASS_PATH.split(pathSeparator))
                                                                 .filter(x -> !x.endsWith(selfJar))
                                                                 .findFirst()
                                                                 .orElse(null)
                                                         : CLASS_PATH;
        }
        return null;
    }
}
