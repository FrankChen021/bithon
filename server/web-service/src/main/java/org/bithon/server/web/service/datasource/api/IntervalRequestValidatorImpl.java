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

package org.bithon.server.web.service.datasource.api;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @author frank.chen021@outlook.com
 * @date 20/4/25 9:39 pm
 */
public class IntervalRequestValidatorImpl implements ConstraintValidator<IntervalRequestValidator, IntervalRequest> {
    @Override
    public boolean isValid(IntervalRequest interval, ConstraintValidatorContext context) {
        if (interval == null) {
            return true;
        }

        if (interval.getStep() != null && interval.getStep() < 1) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("step parameter must be greater than 0")
                   .addConstraintViolation();
            return false;
        }

        if (interval.getWindow() != null
            && interval.getStep() != null
            && interval.getWindow().getDuration().getSeconds() < interval.getStep()
        ) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("window parameter must be greater than or equal to the step")
                   .addConstraintViolation();
            return false;
        }

        return true;
    }

}
