package com.sbss.bithon.agent.core.plugin.debug;


import com.sbss.bithon.agent.core.context.AgentContext;
import shaded.net.bytebuddy.agent.builder.AgentBuilder;
import shaded.net.bytebuddy.description.type.TypeDescription;
import shaded.net.bytebuddy.dynamic.DynamicType;
import shaded.net.bytebuddy.utility.JavaModule;
import shaded.org.slf4j.Logger;
import shaded.org.slf4j.LoggerFactory;

import java.io.File;

import static java.io.File.separator;

/**
 * @author frankchen
 */
public class TransformationDebugger extends AgentBuilder.Listener.Adapter {
    private static final Logger log = LoggerFactory.getLogger(TransformationDebugger.class);

    private final File classRootPath;

    public TransformationDebugger() {
        this.classRootPath = new File(AgentContext.getInstance().getAgentDirectory() + separator + AgentContext.TMP_DIR + separator + "debugging");
        try {
            if (!classRootPath.exists()) {
                classRootPath.mkdir();
            }
        } catch (Exception e) {
            log.error("log error", e);
        }
    }

    synchronized public void saveClassToFile(DynamicType dynamicType) {
        try {
            log.info("[{}] Saved to [{}]", dynamicType.getTypeDescription().getTypeName(), classRootPath);
            dynamicType.saveIn(classRootPath);
        } catch (Throwable e) {
            log.warn("Failed to save class {} to file." + dynamicType.getTypeDescription().getActualName(), e);
        }
    }

    @Override
    public void onTransformation(TypeDescription typeDescription,
                                 ClassLoader classLoader,
                                 JavaModule javaModule,
                                 boolean loaded, DynamicType dynamicType) {
        log.info("{} Transformed", typeDescription.getTypeName());
        saveClassToFile(dynamicType);
    }

    @Override
    public void onError(String s,
                        ClassLoader classLoader,
                        JavaModule javaModule,
                        boolean b,
                        Throwable throwable) {
        log.error(String.format("Failed to transform %s", s), throwable);
    }
}
