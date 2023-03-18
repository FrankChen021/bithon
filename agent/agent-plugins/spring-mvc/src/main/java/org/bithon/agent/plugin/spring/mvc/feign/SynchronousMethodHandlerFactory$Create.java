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

package org.bithon.agent.plugin.spring.mvc.feign;

import feign.MethodMetadata;
import feign.Request;
import feign.RequestTemplate;
import feign.Target;
import feign.codec.Decoder;
import feign.codec.ErrorDecoder;
import org.bithon.agent.bootstrap.aop.AbstractInterceptor;
import org.bithon.agent.bootstrap.aop.AopContext;
import org.bithon.agent.bootstrap.aop.IBithonObject;

/**
 * {@link feign.SynchronousMethodHandler.Factory#create(Target, MethodMetadata, RequestTemplate.Factory, Request.Options, Decoder, ErrorDecoder)}
 */
public class SynchronousMethodHandlerFactory$Create extends AbstractInterceptor {

    /**
     * MethodMeta object in {@link feign.SynchronousMethodHandler} is defined private,
     * This interceptor set this private object on our interface so that the interceptor for the handler access this field more easily.
     * <p>
     * see {@link SynchronousMethodHandler$Invoke}
     */
    @Override
    public void after(AopContext aopContext) {
        if (aopContext.hasException()) {
            return;
        }

        Object methodHandler = aopContext.getReturning();
        if (methodHandler instanceof IBithonObject) {
            // arg1 is MethodMeta
            ((IBithonObject) methodHandler).setInjectedObject(
                new FeignInvocationContext((MethodMetadata) aopContext.getArgs()[1],
                                           (Target<?>) aopContext.getArgs()[0])
            );
        }
    }
}
