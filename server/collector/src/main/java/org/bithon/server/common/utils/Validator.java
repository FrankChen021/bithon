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

package org.bithon.server.common.utils;

import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.groups.Default;
import java.util.Set;

/**
 * @author frank.chen021@outlook.com
 * @date 2020/12/29
 */
public class Validator {
    public static void validate(Object obj) {
        javax.validation.Validator validator = Validation.buildDefaultValidatorFactory()
                                                         .getValidator();

        Set<ConstraintViolation<Object>> set = validator.validate(obj, Default.class);
        if (!CollectionUtils.isEmpty(set)) {
            throw new IllegalArgumentException(set.stream().findFirst().get().getMessage());
        }
    }
}
