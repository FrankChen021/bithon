package com.sbss.bithon.agent.bootstrap;

import com.sbss.bithon.agent.core.config.AgentConfig;
import com.sbss.bithon.agent.core.context.AgentContext;
import com.sbss.bithon.agent.core.plugin.AbstractPlugin;
import com.sbss.bithon.agent.core.plugin.loader.AgentClassloader;
import com.sbss.bithon.agent.core.plugin.loader.BootstrapAopInstaller;
import com.sbss.bithon.agent.core.plugin.loader.PluginInterceptorInstaller;
import com.sbss.bithon.agent.core.plugin.loader.PluginResolver;
import com.sbss.bithon.agent.core.setting.AgentSettingManager;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.org.apache.log4j.xml.DOMConfigurator;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.sbss.bithon.agent.bootstrap.Constant.ManifestAttribute.SPRING_BOOT_CLASSES;
import static com.sbss.bithon.agent.bootstrap.Constant.VMOption.JAVA_CLASS_PATH;
import static java.io.File.pathSeparator;
import static java.io.File.separator;

class AgentStarter {
    private static final Logger log = LoggerFactory.getLogger(AgentStarter.class);

    private static final String CONF_LOG_FILE = "log4j.xml";
    private static final String CLASS_PATH = System.getProperty(JAVA_CLASS_PATH.value());
    private static final String CATALINA_HOME = System.getProperty(Constant.VMOption.CATALINA_HOME.value());
    private final String agentPath;

    AgentStarter() {
        agentPath = new File(AgentStarter.class.getProtectionDomain()
                                               .getCodeSource()
                                               .getLocation()
                                               .getFile()).getParentFile().getPath();
    }

    void start(Instrumentation inst) throws Exception {
        // init log
        DOMConfigurator.configure(agentPath + separator + AgentContext.CONF_DIR + separator + CONF_LOG_FILE);

        AgentContext agentContext = AgentContext.createInstance(agentPath);

        // init setting
        AgentSettingManager.createInstance(agentContext.getAppInstance(),
                                           agentContext.getConfig().getFetcher());

        //
        loadContext(agentContext.getConfig());

        AgentClassloader.createInstance();

        List<AbstractPlugin> plugins = new PluginResolver(agentPath).resolve();
        AgentBuilder agentBuilder = new BootstrapAopInstaller(inst,
                                                              createDefaultAgentBuilder(inst)).install(plugins);

        new PluginInterceptorInstaller(agentBuilder, inst).install(plugins);

        plugins.forEach((plugin) -> {
            plugin.start();
        });

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            plugins.forEach((plugin) -> {
                plugin.stop();
            });
        }));
    }

    private AgentBuilder.Default createDefaultAgentBuilder(Instrumentation inst) {
        return new AgentBuilder.Default();
    }

    private void loadContext(AgentConfig config) throws Exception {
        File tmpDirs = findTempDir(config);
        if (!tmpDirs.exists()) {
            // noinspection ResultOfMethodCallIgnored
            tmpDirs.mkdirs();
        }

        // springBoot project
        if (null == CATALINA_HOME || CATALINA_HOME.trim().length() <= 0) {
            logSeparate(config);
        }
    }

    private File findTempDir(AgentConfig config) {
        return new File(agentPath + separator + AgentContext.TMP_DIR + separator +
                        config.getBootstrap().getAppName());
    }

    private void logSeparate(AgentConfig config) throws IOException {
        String targetJar = getTargetJar();
        if (new File(targetJar).isDirectory()) {
            return;
        }
        try (JarFile jarFile = new JarFile(targetJar)) {
            String classesDir = jarFile.getManifest().getMainAttributes().getValue(SPRING_BOOT_CLASSES.value());
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
                String key = Constant.ProgramOption.LOGGING_CONFIG.value();
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
