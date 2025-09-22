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
class ImplementsMatcher<T extends MethodDescription> extends ElementMatcher.Junction.AbstractBase<T> {

    private final Set<String> baseType;

    /**
     * Cached declared methods in the base type.
     */
    private Set<String> declaredMethods;

    ImplementsMatcher(String... baseType) {
        this.baseType = new HashSet<>(Arrays.asList(baseType));
    }

    /**
     * Get the declared methods in given base type
     */
    private Set<String> getDeclaredMethods(T targetMethod) {
        if (declaredMethods != null) {
            return declaredMethods;
        }

        declaredMethods = new HashSet<>();
        {
            Set<String> processedTypes = new HashSet<>();

            // Find all interfaces/parent classes that are the 'baseType' type
            for (TypeDefinition declaringType : targetMethod.getDeclaringType()) {

                // Search super interfaces
                searchSuperInterface(declaringType.getInterfaces(), processedTypes, declaredMethods);
            }
        }

        return declaredMethods;
    }

    private void searchSuperInterface(List<? extends TypeDefinition> superInterfaces, Set<String> processedTypes, Set<String> foundMethods) {
        for (TypeDefinition superInterface : superInterfaces) {
            if (baseType.contains(superInterface.asErasure().getName())) {
                // Collect all methods declared in this interface and its super interfaces
                collectDeclaredMethods(superInterface.asErasure(), processedTypes, foundMethods);
            } else {
                // Search super interfaces recursively
                searchSuperInterface(superInterface.getInterfaces(), processedTypes, foundMethods);
            }
        }
    }

    private void collectDeclaredMethods(
        TypeDefinition declaringType,
        Set<String> processedType,
        Set<String> foundMethods
    ) {
        if (!processedType.add(declaringType.asErasure().getName())) {
            return;
        }

        for (MethodDescription method : declaringType.getDeclaredMethods()) {
            if (method.isVirtual()) {
                foundMethods.add(method.asSignatureToken().toString());
            }
        }

        // Recursively collect methods from super interfaces
        for (TypeDefinition superInterface : declaringType.getInterfaces()) {
            collectDeclaredMethods(superInterface.asErasure(), processedType, foundMethods);
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
