package com.sbss.bithon.agent.bootstrap;

import com.sbss.bithon.agent.bootstrap.provider.AgentPathProvider;
import com.sbss.bithon.agent.core.config.CoreConfig;
import com.sbss.bithon.agent.core.interceptor.AroundMethodInterceptor;
import com.sbss.bithon.agent.core.interceptor.ConstructorInterceptor;
import com.sbss.bithon.agent.core.interceptor.StaticMethodInterceptor;
import com.sbss.bithon.agent.core.loader.AgentClassloader;
import com.sbss.bithon.agent.core.loader.AgentClassloaderManager;
import com.sbss.bithon.agent.core.transformer.AbstractClassTransformer;
import com.sbss.bithon.agent.core.transformer.debug.InstrumentDebuggingClass;
import com.sbss.bithon.agent.core.util.YamlUtil;
import com.sbss.bithon.agent.dispatcher.ktrace.DispatchTraceProcessor;
import com.sbss.bithon.agent.dispatcher.metrics.DispatchProcessor;
import org.omg.PortableInterceptor.Interceptor;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.ClassFileLocator;
import shaded.net.bytebuddy.dynamic.loading.ClassInjector;
import shaded.org.apache.log4j.xml.DOMConfigurator;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static com.sbss.bithon.agent.bootstrap.Constant.ManifestAttribute.SPRING_BOOT_CLASSES;
import static com.sbss.bithon.agent.bootstrap.Constant.ManifestAttribute.TRANSFORMER_CLASS;
import static com.sbss.bithon.agent.bootstrap.Constant.Suffix.JAR;
import static com.sbss.bithon.agent.bootstrap.Constant.VMOption.*;
import static com.sbss.bithon.agent.core.Constant.*;
import static java.io.File.pathSeparator;
import static java.io.File.separator;

class AgentProcessor {

    private static final Logger log = LoggerFactory.getLogger(AgentProcessor.class);

    private static String classPath = System.getProperty(JAVA_CLASS_PATH.value());
    private static String conf = System.getProperty(CONF.value());
    private static String catalinaHome = System.getProperty(CATALINA_HOME.value());
    private final String agentPath = AgentPathProvider.getAgentPath();

    private File bootstrapClassloaderTmpDir;

    private AgentBuilder.Default agentBuilder;

    void process(Instrumentation inst) throws Exception {
        init().loadContext();
        bootstrapClassloaderTmpDir = createBootstrapClassTempDir();
        AgentClassloaderManager.setAgentPath(agentPath);
        initByteBuddyAgent(inst);
        for (File jarLib : findPluginJarLibs()) {
            transform(jarLib, inst);
        }
    }

    private AgentProcessor init() throws Exception {
        initLogConfig();
        initAgentConfig();

        initDebug();

        DispatchProcessor.createInstance(agentPath);
        DispatchTraceProcessor.createInstance(agentPath);
        return this;
    }

    private void initLogConfig() {
        DOMConfigurator.configure(agentPath + separator + CONF_DIR + separator + CONF_LOG_FILE);
    }

    private void initAgentConfig() throws Exception {
        File yml = new File(null != conf && conf.trim().length() > 0 ? conf :
                                agentPath + separator + CONF_DIR + separator + CONF_CORE_FILE);
        log.info(String.format("Target config: %s", yml.getAbsolutePath()));
        CoreConfig config = YamlUtil.load(yml, CoreConfig.class);
        if (config.getBootstrap().getAppName() == null || "".equals(config.getBootstrap().getAppName())) {
            Map<String, String> map = System.getenv();
            String containerName = map.get("CONTAINER_NAME");
            String currentEnv = map.get("CURRENT_ENV");

            if (isEmpty(containerName)) {
                throw new RuntimeException("can't get CONTAINER_NAME");
            }
            if (isEmpty(currentEnv)) {
                throw new RuntimeException("can't get CURRENT_ENV");
            }
            config.getBootstrap().setEnv(currentEnv);
            config.getBootstrap().setAppName(containerName + "-" + currentEnv);
        }

        CoreConfig.setInstance(config);
    }

    private boolean isEmpty(String v) {
        return v == null || v.isEmpty();
    }

    private void initDebug() {
        InstrumentDebuggingClass.init(agentPath + separator + TMP_DIR + separator + CoreConfig.getInstance().getBootstrap().getAppName());
    }

    /**
     * 直接在Agent创建时, 创建唯一的agentBuilder实例, 同时定义这个方法可以得到扩展, 比如在此处定义通用的agentListener等
     */
    private void initByteBuddyAgent(Instrumentation inst) {
        // 将切面类缓存至bootstrap class tmp, 这样就可以用这些tmp文件对bootstrapClassLoader加载的类做增强
        Map<TypeDescription, byte[]> bootstrapEnhanceClassTmpMap = new HashMap<>();
        bootstrapEnhanceClassTmpMap.put(new TypeDescription.ForLoadedType(Interceptor.class),
                                        ClassFileLocator.ForClassLoader.read(Interceptor.class).resolve());
        bootstrapEnhanceClassTmpMap.put(new TypeDescription.ForLoadedType(AroundMethodInterceptor.class),
                                        ClassFileLocator.ForClassLoader.read(AroundMethodInterceptor.class).resolve());
        bootstrapEnhanceClassTmpMap.put(new TypeDescription.ForLoadedType(ConstructorInterceptor.class),
                                        ClassFileLocator.ForClassLoader.read(ConstructorInterceptor.class).resolve());
        bootstrapEnhanceClassTmpMap.put(new TypeDescription.ForLoadedType(StaticMethodInterceptor.class),
                                        ClassFileLocator.ForClassLoader.read(StaticMethodInterceptor.class).resolve());
        ClassInjector.UsingInstrumentation.of(bootstrapClassloaderTmpDir,
                                              ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, inst).inject(bootstrapEnhanceClassTmpMap);

        this.agentBuilder = new AgentBuilder.Default();
    }

    private void loadContext() throws Exception {
        File tmpDirs = findTempDir();
        if (!tmpDirs.exists()) {
            //noinspection ResultOfMethodCallIgnored
            tmpDirs.mkdirs();
        }

        // springBoot project
        if (null == catalinaHome || catalinaHome.trim().length() <= 0) {
            logSeparate();
        }
    }

    private File[] findPluginJarLibs() {
        String pluginPath = agentPath + separator + PLUGIN_DIR;
        return new File(pluginPath).listFiles(file -> file.getName().toLowerCase().endsWith(JAR.value()));
    }

    private void transform(File jarLib, Instrumentation inst) throws Exception {
        // 尝试将每个插件加入AgentBuilder并创建hook, 在得到匹配时, 动态使用对应切点classloader加载plugin, inspired by pinpoint & skyWalking
        // 这种方式的实现中, 插件的加载过程将被分裂成两个过程
        // 1. 使用Agent Classloader加载transformer, 也就是插件的配置信息, 并将这些信息加载到AgentBuilder, 以形成方法切面
        // 2. 在此同时, 将handler, 也就是增强方法的名称写入切面, 在拦截到切点的同时, 以切点classloader加载handler

        // 获取默认AgentClassloader, 以保证能够搜索到需要的plugin文件
        ClassLoader classLoader = AgentClassloader.getDefaultInstance();

        JarFile jar = new JarFile(jarLib);
        String transformerClass = jar.getManifest().getMainAttributes().getValue(TRANSFORMER_CLASS.value());
        jar.close();
        AbstractClassTransformer transformer =
            (AbstractClassTransformer) Class.forName(transformerClass, true, classLoader).newInstance();
        transformer.enhance(agentBuilder, bootstrapClassloaderTmpDir, inst);
    }

    private File findTempDir() {
        return new File(agentPath + separator + TMP_DIR + separator + CoreConfig.getInstance().getBootstrap().getAppName());
    }

    /**
     * 创建bootstrapClassTempDir备用
     */
    private File createBootstrapClassTempDir() throws IOException {
        String path =
            agentPath + separator + TMP_DIR + separator + CoreConfig.getInstance().getBootstrap().getAppName();
        Path dir = Paths.get(path);
        return Files.createTempDirectory(dir, BOOTSTRAP_CLASS_TMP_DIR).toFile();
    }

    private void logSeparate() throws IOException {
        String targetJar = parseTargetJar();
        if (new File(targetJar).isDirectory()) {
            return;
        }
        JarFile jarFile = new JarFile(targetJar);
        String classesDir = jarFile.getManifest().getMainAttributes().getValue(SPRING_BOOT_CLASSES.value());
        if (classesDir == null) {
            jarFile.close();
            return;
        }

        Optional<JarEntry> classesEntry =
            jarFile.stream().filter(e -> e.getName().startsWith(classesDir) && CoreConfig.getInstance().getBootstrap().getAppLogs().contains(
                e.getName().substring(e.getName().lastIndexOf("/") + 1)
            )).findFirst();
        log.info("SpringBoot log config file name: " + classesEntry);
        classesEntry.ifPresent(e -> {
            String key = Constant.ProgramOption.LOGGING_CONFIG.value();
            String fileName = e.getName().substring(e.getName().lastIndexOf("/") + 1);
            String value = String.format("classpath:%s", fileName);
            log.info(String.format("Set system properties [%s = %s]", key, value));
            System.setProperty(key, value);
        });
        jarFile.close();
    }

    private String parseTargetJar() {
        String result = null;
        String selfJar =
            new File(AgentProcessor.class.getProtectionDomain().getCodeSource().getLocation().getFile()).getName();
        if (null != classPath && classPath.trim().length() > 0) {
            result = classPath.indexOf(pathSeparator) > 0
                ?
                Arrays.stream(classPath.split(pathSeparator)).filter(x -> !x.endsWith(selfJar)).findFirst().orElse(null)
                :
                classPath;
        }
        return result;
    }
}
