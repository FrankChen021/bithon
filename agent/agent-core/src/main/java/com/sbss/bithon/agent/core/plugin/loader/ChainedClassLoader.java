package com.sbss.bithon.agent.core.plugin.loader;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/3/31 22:02
 */
public class ChainedClassLoader extends ClassLoader {
    ClassLoader[] parents;

    public ChainedClassLoader(ClassLoader... parents) {
        this.parents = parents;
    }


    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        for (int i = 0; i < parents.length; i++) {
            try {
                return parents[i].loadClass(name);
            } catch (ClassNotFoundException e) {
            }
        }
        throw new ClassNotFoundException(name);
    }

    @Override
    protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
        return loadClass(name);
    }
}
