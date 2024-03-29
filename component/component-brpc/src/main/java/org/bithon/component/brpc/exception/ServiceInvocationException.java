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

import java.util.Locale;

/**
 * @author frankchen
 */
public class ServiceInvocationException extends RuntimeException {
    public ServiceInvocationException(CharSequence message) {
        super(message instanceof String ? (String) message : message.toString());
    }

    public ServiceInvocationException(String messageFormat, Object... args) {
        super(String.format(Locale.ENGLISH, messageFormat, args));
    }

    public ServiceInvocationException(Throwable cause, String messageFormat, Object... args) {
        super(String.format(Locale.ENGLISH, messageFormat, args), cause);
    }
}
