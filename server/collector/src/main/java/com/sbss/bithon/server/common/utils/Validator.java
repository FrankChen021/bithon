package com.sbss.bithon.server.common.utils;

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
