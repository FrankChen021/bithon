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

package org.bithon.agent.instrumentation.aop.interceptor.matcher;

import org.bithon.shaded.net.bytebuddy.description.method.MethodDescription;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDefinition;
import org.bithon.shaded.net.bytebuddy.matcher.ElementMatcher;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/7/19 13:18
 */
public class IsMethodOverriddenFrom<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final Set<String> baseType;

    /**
     * Cached declared methods in the base type.
     */
    private Set<String> declaredMethods;

    public IsMethodOverriddenFrom(String... baseType) {
        this.baseType = new HashSet<>(Arrays.asList(baseType));
    }

    /**
     * Get the declared methods in given base type
     */
    private Set<String> getDeclaredMethods(T target) {
        if (declaredMethods != null) {
            return declaredMethods;
        }

        declaredMethods = new HashSet<>();
        {
            Set<String> processedInterfaces = new HashSet<>();
            for (TypeDefinition declaringType : target.getDeclaringType()) {
                List<? extends TypeDefinition> declaringInterfaces = declaringType.getInterfaces();
                for (TypeDefinition declaringInterface : declaringInterfaces) {
                    if (baseType.contains(declaringInterface.asErasure().getName())) {
                        collectDeclaredMethods(declaringInterface, processedInterfaces, declaredMethods);
                        if (baseType.size() == 1) {
                            // If only one base type is specified, we can stop here
                            return declaredMethods;
                        }
                    }
                }
            }
        }

        return declaredMethods;
    }

    private void collectDeclaredMethods(TypeDefinition declaringInterface,
                                        Set<String> processedType,
                                        Set<String> declaredMethods) {
        if (!processedType.add(declaringInterface.asErasure().getName())) {
            return;
        }

        for (MethodDescription method : declaringInterface.getDeclaredMethods()) {
            if (method.isVirtual()) {
                declaredMethods.add(method.asSignatureToken().toString());
            }
        }

        // Recursively collect methods from super interfaces
        for (TypeDefinition superInterface : declaringInterface.getInterfaces()) {
            collectDeclaredMethods(superInterface.asErasure(), processedType, declaredMethods);
        }
    }

    /**
     * {@inheritDoc}
     */
    public boolean matches(T target) {
        if (!target.isMethod()) {
            return false;
        }

        Set<String> declaredMethods = getDeclaredMethods(target);
        if (declaredMethods != null && !declaredMethods.isEmpty()) {
            return declaredMethods.contains(target.asSignatureToken().toString());
        }

        return false;
    }

    @Override
    public String toString() {
        return "IsMethodOverriddenFrom(" + baseType + ")";
    }
}
