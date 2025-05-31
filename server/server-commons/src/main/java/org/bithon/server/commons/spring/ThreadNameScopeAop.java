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

package org.bithon.server.commons.spring;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.util.regex.Pattern;

/**
 * AspectJ implementation for the @ThreadName annotation.
 * This aspect intercepts methods annotated with @ThreadName, temporarily changes
 * the current thread's name during method execution, and restores the original
 * thread name after the method completes (whether successfully or with an exception).
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
@Aspect
public class ThreadNameScopeAop {

    @Around("@annotation(org.bithon.server.commons.spring.ThreadNameScope)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // Get the method and annotation
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        ThreadNameScope scopeAnnotation = method.getAnnotation(ThreadNameScope.class);

        if (scopeAnnotation == null) {
            // Should not happen, but safety check
            return joinPoint.proceed();
        }

        // Get current thread and its original name
        Thread currentThread = Thread.currentThread();
        String oldThreadName = currentThread.getName();

        try {
            // Set the new thread name
            currentThread.setName(determineThreadName(oldThreadName, scopeAnnotation));

            // Proceed with the original method execution
            return joinPoint.proceed();
        } finally {
            currentThread.setName(oldThreadName);
        }
    }

    /**
     * Determine the new thread name based on the annotation values.
     * If template is provided, it's used as a regex pattern to replace parts of the current thread name.
     * If no match occurs, the value is used directly as the thread name.
     * Otherwise, the value is used directly as the new thread name.
     */
    String determineThreadName(String currentThreadName, ThreadNameScope annotation) {
        String value = annotation.value();
        String template = annotation.template();

        if (template == null || template.trim().isEmpty()) {
            // No template, use value directly
            return value;
        }

        try {
            // Use template as regex pattern and value as replacement
            Pattern pattern = Pattern.compile(template);
            if (pattern.matcher(currentThreadName).find()) {
                // Template matches, apply replacement
                return pattern.matcher(currentThreadName).replaceAll(value);
            } else {
                // Template doesn't match, use value directly
                return value;
            }
        } catch (Exception e) {
            // If regex processing fails, fall back to using value directly
            return value;
        }
    }
} 
