package com.sbss.bithon.agent.core.loader;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Description : 原始classloader -> 自定义plugin classloader的映射表, 统一管理, 一对一,
 * 这样就不用创建太多多余的classloader <br>
 * Date: 17/12/1
 *
 * @author 马至远
 */
public final class AgentClassloaderManager {
    private static Map<ClassLoader, ClassLoader> classloaderMapping = new ConcurrentHashMap<>();

    private static String agentPath;

    public static ClassLoader getMappingClassloader(ClassLoader classloader) {
        // 尝试获取或创建(并加入mapping)当前classloader的子类agentClassloader,
        // agentClassloader的作用是可以正确的搜索到目标handler文件
        return classloaderMapping.computeIfAbsent(classloader, k -> {
            try {
                return new AgentClassloader(classloader);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * 如果需要直接注册classloader映射关系
     *
     * @param originClassloader 原始classloader
     * @param agentClassloader  代理classloader
     */
    public static void register(ClassLoader originClassloader,
                                ClassLoader agentClassloader) {
        classloaderMapping.putIfAbsent(originClassloader, agentClassloader);
    }

    public static void setAgentPath(String a) {
        agentPath = a;
    }

    public static String getAgentPath() {
        return agentPath;
    }
}
