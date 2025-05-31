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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for methods that need to execute with a specific thread name.
 * The thread name will be temporarily changed during method execution
 * and restored after the method completes.
 *
 * @author frank.chen021@outlook.com
 * @date 2024/12/19
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ThreadNameScope {
    
    /**
     * The thread name to use during method execution, or the replacement string
     * when used with a template.
     *
     * @return the thread name or replacement string
     */
    String value();
    
    /**
     * Optional regular expression pattern to match against the current thread name.
     * If provided, any parts of the current thread name matching this pattern
     * will be replaced with the value().
     * <p>
     * If template is not provided or empty, the value() will be used directly
     * as the new thread name.
     * <p>
     * Example: template = "^http-nio-(\d+)-" would match thread names like
     * "http-nio-8080-exec-1" and replace matched part with the value().
     * If the value() is "myThread-", the new thread name would be myThread-exec-1
     *
     * @return the regex pattern, empty string by default
     */
    String template() default "";
} 
