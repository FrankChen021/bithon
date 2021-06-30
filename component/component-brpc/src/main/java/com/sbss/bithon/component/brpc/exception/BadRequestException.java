/*
 *    Copyright 2020 bithon.cn
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

package com.sbss.bithon.component.brpc.exception;

/**
 * used only at server side for code simplification, and should not be used at client side
 */
public class BadRequestException extends ServiceInvocationException {
    public BadRequestException(String message) {
        super("Bad Request:" + message);
    }

    public BadRequestException(String messageFormat, Object... args) {
        super(messageFormat, args);
    }
}
