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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Checks the implementation version exposed by the package of a class.
 *
 * @author frankchen
 */
public class ClassPackageVersionPrecondition implements IInterceptorPrecondition {
    private final Map<ClassLoader, EvaluationResult> evaluationResult = Collections.synchronizedMap(
        new WeakHashMap<>()
    );
    private final String className;
    private final PropertyFileValuePrecondition.PropertyValuePredicate valuePredicate;

    public ClassPackageVersionPrecondition(String className,
                                           PropertyFileValuePrecondition.PropertyValuePredicate valuePredicate) {
        this.className = className;
        this.valuePredicate = valuePredicate;
    }

    @Override
    public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
        synchronized (this.evaluationResult) {
            EvaluationResult result = this.evaluationResult.get(classLoader);
            if (result == null) {
                result = matches(classLoader);
                this.evaluationResult.put(classLoader, result);
            }
            return result.matched;
        }
    }

    private EvaluationResult matches(ClassLoader classLoader) {
        try {
            Class<?> clazz = Class.forName(this.className, false, classLoader);
            Package pkg = clazz.getPackage();
            if (pkg == null) {
                return EvaluationResult.NOT_MATCHED;
            }
            String actual = pkg.getImplementationVersion();
            return new EvaluationResult(actual != null && this.valuePredicate.matches(actual));
        } catch (Throwable ignored) {
            return EvaluationResult.NOT_MATCHED;
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                             "'%s'[packageImplementationVersion] %s",
                             this.className,
                             this.valuePredicate);
    }

    private static class EvaluationResult {
        private static final EvaluationResult NOT_MATCHED = new EvaluationResult(false);

        private final boolean matched;

        private EvaluationResult(boolean matched) {
            this.matched = matched;
        }
    }
}
