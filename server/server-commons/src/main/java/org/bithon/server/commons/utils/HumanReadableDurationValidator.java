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

package org.bithon.server.commons.utils;

import org.bithon.component.commons.utils.HumanReadableDuration;
import org.bithon.component.commons.utils.StringUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;

/**
 * @author frank.chen021@outlook.com
 * @date 2024/2/11 15:07
 */
public class HumanReadableDurationValidator implements ConstraintValidator<HumanReadableDurationConstraint, HumanReadableDuration> {

    private long max = Integer.MAX_VALUE;
    private long min;

    @Override
    public void initialize(HumanReadableDurationConstraint constraintAnnotation) {
        String max = constraintAnnotation.max();
        if (StringUtils.hasText(max)) {
            this.max = HumanReadableDuration.parse(max).getDuration().getSeconds();
        }
        this.min = HumanReadableDuration.parse(constraintAnnotation.min()).getDuration().getSeconds();
    }

    @Override
    public boolean isValid(HumanReadableDuration value, ConstraintValidatorContext context) {
        long seconds = value.getDuration().getSeconds();
        if (seconds < this.min) {
            return false;
        }
        if (seconds > this.max) {
            return false;
        }
        return true;
    }
}
