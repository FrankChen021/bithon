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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bithon.agent.instrumentation.utils.VersionUtils;
import org.bithon.shaded.net.bytebuddy.description.type.TypeDescription;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import java.util.Properties;

/**
 * @author frank.chen021@outlook.com
 * @date 2025/1/6 23:58
 */
public class PropertyFileValuePrecondition implements IInterceptorPrecondition {
    protected final String propertyFile;
    protected final String propertyName;
    protected final PropertyValuePredicate valuePredicate;

    public interface PropertyValuePredicate {
        boolean matches(String actual);
    }

    public static PropertyValuePredicate AND(PropertyValuePredicate... predicates) {
        return new And(predicates);
    }

    private static class And implements PropertyValuePredicate {
        private final PropertyValuePredicate[] predicates;

        public And(PropertyValuePredicate[] predicates) {
            this.predicates = predicates;
        }

        @Override
        public boolean matches(String actual) {
            for (PropertyValuePredicate predicate : predicates) {
                if (!predicate.matches(actual)) {
                    return false;
                }
            }
            return true;
        }
    }

    public static class StringEQ implements PropertyValuePredicate {
        private final String expected;

        private StringEQ(String expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(String actual) {
            return expected.equals(actual);
        }

        @Override
        public String toString() {
            return "= '" + expected + "'";
        }

        public static PropertyValuePredicate of(String expected) {
            return new StringEQ(expected);
        }
    }

    public static class VersionLT implements PropertyValuePredicate {
        protected final String expected;

        private VersionLT(String expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(String actual) {
            return VersionUtils.compare(actual, expected) < 0;
        }

        @Override
        public String toString() {
            return "> '" + expected + "'";
        }

        public static PropertyValuePredicate of(String expected) {
            return new VersionGTE(expected);
        }
    }

    public static class VersionGT implements PropertyValuePredicate {
        protected final String expected;

        private VersionGT(String expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(String actual) {
            return VersionUtils.compare(actual, expected) > 0;
        }

        @Override
        public String toString() {
            return "> '" + expected + "'";
        }

        public static PropertyValuePredicate of(String expected) {
            return new VersionGTE(expected);
        }
    }

    public static class VersionGTE implements PropertyValuePredicate {
        protected final String expected;

        private VersionGTE(String expected) {
            this.expected = expected;
        }

        @Override
        public boolean matches(String actual) {
            return VersionUtils.compare(actual, expected) >= 0;
        }

        @Override
        public String toString() {
            return ">= '" + expected + "'";
        }

        public static PropertyValuePredicate of(String expected) {
            return new VersionGTE(expected);
        }
    }

    public PropertyFileValuePrecondition(String propertyFile,
                                         String propertyName,
                                         PropertyValuePredicate valuePredicate) {
        this.propertyFile = propertyFile;
        this.propertyName = propertyName;
        this.valuePredicate = valuePredicate;
    }

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_OF_NULL_VALUE")
    @Override
    public boolean matches(ClassLoader classLoader, TypeDescription typeDescription) {
        try (InputStream inputStream = classLoader.getResourceAsStream(propertyFile)) {
            if (inputStream == null) {
                return false;
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            String value = properties.getProperty(propertyName);
            return this.valuePredicate.matches(value);
        } catch (IOException ignored) {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format(Locale.ENGLISH,
                             "'%s'[%s] %s",
                             propertyFile,
                             propertyName,
                             valuePredicate);
    }
}
