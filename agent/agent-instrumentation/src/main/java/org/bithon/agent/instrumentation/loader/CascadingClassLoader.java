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

package org.bithon.agent.instrumentation.loader;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * @author frank.chen021@outlook.com
 * @date 28/12/21 10:31 AM
 */
public class CascadingClassLoader extends ClassLoader {
    private final ClassLoader[] parents;

    public CascadingClassLoader(ClassLoader... parents) {
        // NOTE:  parent is assigned to parent class loader
        // This is the key to implement agent lib isolation from app libs
        super(null);
        this.parents = parents;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
        for (ClassLoader parent : parents) {
            try {
                if (parent != this) {
                    // parent is a provider, it could be set dynamically to be instance of current class
                    return parent.loadClass(name);
                }
            } catch (ClassNotFoundException ignored) {
            }
        }
        throw new ClassNotFoundException(String.format(Locale.ENGLISH,
                                                       "%s not found in parents:%s",
                                                       name,
                                                       Arrays.stream(this.parents)
                                                             .map(p -> p.getClass().getName())
                                                             .collect(Collectors.joining(","))));
    }

    @Override
    public URL getResource(String name) {
        // delegate to parent to get resource
        for (ClassLoader parent : parents) {
            if (parent != this) {
                URL url = parent.getResource(name);
                if (url != null) {
                    return url;
                }
            }
        }
        return null;
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return parents[0].getResources(name);
    }
}
