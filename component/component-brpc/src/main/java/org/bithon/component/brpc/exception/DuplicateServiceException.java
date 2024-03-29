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

package org.bithon.component.brpc.exception;

import java.lang.reflect.Method;

/**
 * @author frank.chen021@outlook.com
 * @date 20/10/21 9:50 pm
 */
public class DuplicateServiceException extends ServiceRegistrationException {
    private final Class<?> interfaceClass;
    private final Method method;
    private final String serviceName;
    private final String methodName;

    public DuplicateServiceException(Class<?> interfaceClass,
                                     Method method,
                                     String serviceName,
                                     String methodName,
                                     Method existingMethod) {
        super("Class[%s].[%s] is retrying to register as [%s#%s], but the name has already been registered by [%s].\n"
              + "If you wish to declare the methods with the same name, you need to annotate one of them by BpcMethod to distinguish them.",
              interfaceClass.getTypeName(),
              method.toString(),
              serviceName,
              methodName,
              existingMethod.toString());
        this.interfaceClass = interfaceClass;
        this.method = method;
        this.serviceName = serviceName;
        this.methodName = methodName;
    }

    public String getMethodName() {
        return methodName;
    }

    public String getServiceName() {
        return serviceName;
    }

    public Method getMethod() {
        return method;
    }

    public Class<?> getInterfaceClass() {
        return interfaceClass;
    }
}
