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

package org.bithon.component.brpc;

import org.bithon.component.brpc.message.ServiceMessageType;
import org.bithon.component.brpc.message.serializer.Serializer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Optional annotation on method to override the type level configuration which is {@link BrpcService}
 *
 * @author frankchen
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface BrpcMethod {
    /**
     * Overridden service name
     */
    String name() default "";

    boolean isOneway() default false;

    /**
     * This is mainly for UT compatibility test.
     * Should not be used directly.
     */
    int messageType() default ServiceMessageType.CLIENT_REQUEST_V2;

    Serializer serializer() default Serializer.PROTOBUF;
}
