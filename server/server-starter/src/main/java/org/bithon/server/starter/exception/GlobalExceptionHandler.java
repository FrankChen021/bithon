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

import lombok.extern.slf4j.Slf4j;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.exception.HttpResponseMapping;
import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.server.commons.exception.ErrorResponse;
import org.bithon.server.storage.common.expression.InvalidExpressionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * @author frank.chen021@outlook.com
 * @date 24/3/22 3:05 PM
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({
        BadRequestException.class,
        InvalidExpressionException.class,
        ExpressionValidationException.class,
        HttpMessageNotReadableException.class,
        MethodArgumentNotValidException.class,
        HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class,
        ServletRequestBindingException.class
    })
    public ResponseEntity<ErrorResponse> handleKnownExceptions(HttpServletRequest request, Exception exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
                                                             .traceId((String) request.getAttribute("X-Bithon-TraceId"))
                                                             .traceMode((String) request.getAttribute("X-Bithon-Trace-Mode"))
                                                             .path(request.getRequestURI())
                                                             .message(exception.getMessage())
                                                             .exception(exception.getClass().getName())
                                                             .build());
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, Exception exception) {
        int statusCode;
        HttpResponseMapping mapping = exception.getClass().getDeclaredAnnotation(HttpResponseMapping.class);
        if (mapping == null) {
            if (exception instanceof HttpMappableException) {
                statusCode = ((HttpMappableException) exception).getStatusCode();
            } else {
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

                // Logging unknown exception
                log.error("Unexpected error", exception);
            }
        } else {
            statusCode = mapping.statusCode().value();
        }

        return ResponseEntity.status(statusCode)
                             .body(ErrorResponse.builder()
                                                .traceId((String) request.getAttribute("X-Bithon-TraceId"))
                                                .traceMode((String) request.getAttribute("X-Bithon-Trace-Mode"))
                                                .path(request.getRequestURI())
                                                .exception(exception.getClass().getName())
                                                .message(exception.getMessage())
                                                .build());
    }

}
