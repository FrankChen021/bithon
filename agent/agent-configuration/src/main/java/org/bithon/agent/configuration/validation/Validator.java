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

package org.bithon.agent.configuration.validation;

import org.bithon.component.commons.utils.StringUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * @author frank.chen021@outlook.com
 * @date 2021/8/7 10:37
 */
public class Validator {
    /**
     * Validate the rules declared on each field
     */
    public static String validate(String prefix, Object obj) {

        Map<String, IValueValidator> validators = new HashMap<String, IValueValidator>() {{
            put(NotBlank.class.getName(), new NotBlankValidator());
            put(Range.class.getName(), new RangeValidator());
            put(GreaterThan.class.getName(), new GreaterThanValidator());
            put(RegExpr.class.getName(), new RegExprValidator());

            // Validated is declared on an object, validate the object recursively
            put(Validated.class.getName(), (annotation, objectType, property, value) -> validate(property, value));
        }};

        for (Field field : obj.getClass().getDeclaredFields()) {
            Annotation[] validationAnnotations = field.getDeclaredAnnotations();
            for (Annotation validationAnnotation : validationAnnotations) {
                IValueValidator validator = validators.get(validationAnnotation.annotationType().getName());
                if (validator != null) {
                    String violation = validateField(prefix, validationAnnotation, obj, field, validator);
                    if (violation != null) {
                        return violation;
                    }
                }
            }
        }
        return null;
    }

    private static <T extends Annotation> String validateField(String prefix,
                                                               Annotation declaredValidationAnnotation,
                                                               Object object,
                                                               Field field,
                                                               IValueValidator valueValidator) {
        String property = prefix + '.' + field.getName();
        String violationMessagePattern;
        try {
            // although this function would be called twice if this field has more than 1 validation annotations
            // by putting this call here, we prevent this call on fields with some types that we don't support
            // this prevents following warning/error when using JDK9+
            // WARNING: An illegal reflective access operation has occurred
            field.setAccessible(true);


            violationMessagePattern = valueValidator.validate(declaredValidationAnnotation,
                                                              field.getType(),
                                                              property,
                                                              field.get(object));
            if (violationMessagePattern == null) {
                return null;
            }
        } catch (IllegalAccessException ignored) {
            return null;
        }

        return String.format(Locale.ENGLISH, violationMessagePattern, property);
    }

    interface IValueValidator {
        /**
         * @return violation message pattern
         */
        String validate(Annotation annotation, Class<?> objectType, String property, Object value);
    }

    static class NotBlankValidator implements IValueValidator {
        @Override
        public String validate(Annotation annotation, Class<?> objectType, String property, Object value) {
            if (objectType.equals(String.class)) {
                if (StringUtils.hasText((String) value)) {
                    // success
                    return null;
                } else {
                    return ((NotBlank) annotation).message();
                }
            }
            return null;
        }
    }

    static class RangeValidator implements IValueValidator {
        @Override
        public String validate(Annotation annotation, Class<?> objectType, String property, Object value) {
            if (value instanceof Number) {
                long v = ((Number) value).longValue();
                long min = ((Range) annotation).min();
                long max = ((Range) annotation).max();
                if (v >= min && v <= max) {
                    return null;
                }
                return "[%s] " + String.format(Locale.ENGLISH, "should be in the range of [%d, %d], but is %d", min, max, v);
            }
            return "Type of [%s] is not Number, but " + objectType.getSimpleName();
        }
    }

    static class GreaterThanValidator implements IValueValidator {
        @Override
        public String validate(Annotation annotation, Class<?> objectType, String property, Object value) {
            if (value instanceof Number) {
                long actual = ((Number) value).longValue();
                long expected = ((GreaterThan) annotation).value();
                if (actual > expected) {
                    return null;
                }
                return "[%s] " + String.format(Locale.ENGLISH, "should be greater than [%d], but is %s",
                                               expected,
                                               // Use value.toString() below because the value might be type of HumanReadableNumber
                                               value);
            }
            return "Type of [%s] is not Number, but " + objectType.getSimpleName();
        }
    }

    static class RegExprValidator implements IValueValidator {
        @Override
        public String validate(Annotation annotation, Class<?> objectType, String property, Object value) {
            if (value == null) {
                return StringUtils.format("The [%s] can't be NULL.", property);
            }

            if (!(value instanceof String)) {
                return StringUtils.format("Bug!!! The property [%s] is type of [%s], but the RegExpr annotation can only be applied to a String field.",
                                          property,
                                          value.getClass().getSimpleName());
            }

            RegExpr regExpr = (RegExpr) annotation;
            boolean found = Pattern.compile(regExpr.expr())
                                   .matcher(value.toString())
                                   .find();
            if (found) {
                return null;
            } else {
                // Fail
                return StringUtils.format(((RegExpr) annotation).message(), value);
            }
        }
    }
}
