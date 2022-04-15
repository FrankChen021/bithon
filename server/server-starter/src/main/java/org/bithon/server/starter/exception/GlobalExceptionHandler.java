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

package org.bithon.server.starter.exception;

import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.commons.utils.Preconditions;
import org.bithon.server.storage.datasource.DataSourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Frank Chen
 * @date 24/3/22 3:05 PM
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BadRequestException.class, Preconditions.InvalidValueException.class})
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, RuntimeException exception) {
        log.warn("Caught exception", exception);
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                                                             .path(request.getRequestURI())
                                                             .message(exception.getMessage())
                                                             .build());
    }

    @ExceptionHandler({DataSourceNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, DataSourceNotFoundException exception) {
        log.warn("Caught exception", exception);
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ErrorResponse.builder()
                                                                             .path(request.getRequestURI())
                                                                             .message(exception.getMessage())
                                                                             .build());
    }

    @Data
    @Builder
    public static class ErrorResponse {
        private String path;
        private String message;
    }
}
