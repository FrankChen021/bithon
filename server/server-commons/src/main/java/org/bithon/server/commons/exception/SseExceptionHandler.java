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

package org.bithon.server.commons.exception;


import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;

/**
 * @author frank.chen021@outlook.com
 * @date 27/8/25 8:30 pm
 */
public class SseExceptionHandler {
    public static SseEmitter sendExceptionAndEnd(SseEmitter emitter, String exceptionClass, String exceptionMessage) {
        ErrorResponse error = ErrorResponse.builder()
                                           .exception(exceptionClass)
                                           .message(exceptionMessage)
                                           .build();
        try {
            emitter.send(SseEmitter.event()
                                   .name("error")
                                   .data(error, MediaType.APPLICATION_JSON)
                                   .build());
        } catch (IOException ignored) {
        }
        emitter.complete();

        return emitter;
    }

    public static SseEmitter sendExceptionAndEnd(SseEmitter emitter, Throwable exception) {
        return sendExceptionAndEnd(emitter, exception.getClass().getName(), exception.getMessage());
    }
}
