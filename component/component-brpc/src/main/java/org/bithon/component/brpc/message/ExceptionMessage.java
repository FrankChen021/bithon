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

package org.bithon.component.brpc.message;


import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.brpc.exception.CalleeSideException;
import org.bithon.component.brpc.exception.CallerSideException;
import org.bithon.component.brpc.exception.ServiceInvocationException;
import org.bithon.component.brpc.exception.ServiceNotFoundException;
import org.bithon.shaded.com.google.protobuf.CodedInputStream;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 3/7/25 5:10 pm
 */
public class ExceptionMessage {
    private final String exceptionType;
    private final String exceptionMessage;

    public ExceptionMessage(String exceptionType, String exceptionMessage) {
        this.exceptionType = exceptionType;
        this.exceptionMessage = exceptionMessage;
    }

    public Exception toException() {
        if (BadRequestException.class.getName().equals(exceptionType)) {
            // Compatibility with old clients
            // Turn BadRequestException into ServiceNotFoundException for more accurate exception handling
            String template = "Can't find service provider ";
            int index = exceptionMessage.indexOf(template);
            if (index >= 0) {
                return new ServiceNotFoundException(exceptionMessage.substring(template.length()));
            }

            return new BadRequestException(exceptionMessage);
        } else if (exceptionType.equals(ServiceInvocationException.class.getName())) {
            return new ServiceInvocationException(exceptionMessage);
        } else if (exceptionType.equals(ServiceNotFoundException.class.getName())) {
            return new ServiceNotFoundException(exceptionMessage);
        } else {
            return new CalleeSideException(exceptionType, exceptionMessage);
        }
    }

    public static ExceptionMessage deserializeException(CodedInputStream in) throws IOException {
        boolean hasException = in.readRawByte() == 1;
        if (!hasException) {
            return null;
        }

        String exceptionMessage = in.readString();
        int separator = exceptionMessage.indexOf(' ');
        if (separator <= 0) {
            return new ExceptionMessage(CallerSideException.class.getName(), exceptionMessage);
        }

        String exceptionType = exceptionMessage.substring(0, separator);
        exceptionMessage = exceptionMessage.substring(separator + 1);

        if (!exceptionType.endsWith("Exception")) {
            // According to exception class name convention, it MUST end with 'Exception',
            // Clears the invalid name
            exceptionType = CallerSideException.class.getName();
        }

        return new ExceptionMessage(exceptionType, exceptionMessage);
    }
}
