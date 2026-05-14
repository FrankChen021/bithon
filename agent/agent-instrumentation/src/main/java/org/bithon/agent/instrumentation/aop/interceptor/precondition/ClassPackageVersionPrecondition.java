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

package org.bithon.agent.instrumentation.aop.interceptor.precondition;

import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;

import java.util.Locale;

/**
 * Checks the implementation version exposed by the package of a class.
 *
 * @author frankchen
 */
public class ClassPackageVersionPrecondition implements IInterceptorPrecondition {
    private Boolean evaluationResult;
    private String actual;
    private final String className;
    private final PropertyFileValuePrecondition.PropertyValuePredicate valuePredicate;

    public ClassPackageVersionPrecondition(String className,
                                           PropertyFileValuePrecondition.PropertyValuePredicate valuePredicate) {
        this.className = className;
        this.valuePredicate = valuePredicate;
    }

    @Override
    public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
        if (this.evaluationResult == null) {
            this.evaluationResult = matches(classLoader);
        }
        return this.evaluationResult;
    }

    private boolean matches(ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(this.className, false, classLoader);
            Package pkg = clazz.getPackage();
            if (pkg == null) {
                return false;
            }
            this.actual = pkg.getImplementationVersion();
            return this.actual != null && this.valuePredicate.matches(this.actual);
        } catch (Throwable ignored) {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                             "'%s'[packageImplementationVersion](val=%s) %s",
                             this.className,
                             this.actual,
                             this.valuePredicate);
    }
}
