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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.commons.exception.HttpMappableException;
import org.bithon.component.commons.exception.HttpResponseMapping;
import org.bithon.component.commons.expression.expt.InvalidExpressionException;
import org.bithon.component.commons.expression.validation.ExpressionValidationException;
import org.bithon.component.commons.utils.StringUtils;
import org.bithon.server.commons.exception.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.io.IOException;
import java.util.Optional;


/**
 * @author frank.chen021@outlook.com
 * @date 24/3/22 3:05 PM
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    private final ObjectMapper objectMapper;

    public GlobalExceptionHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @ExceptionHandler({
        MethodArgumentNotValidException.class,
    })
    public ResponseEntity<?> handleKnownExceptions(HttpServletRequest request,
                                                   HttpServletResponse response,
                                                   MethodArgumentNotValidException exception) {
        FieldError error = exception.getBindingResult().getFieldError();
        ErrorResponse errorResponse = ErrorResponse.builder()
                                                   .traceId((String) request.getAttribute("X-Bithon-TraceId"))
                                                   .traceMode((String) request.getAttribute("X-Bithon-Trace-Mode"))
                                                   .path(request.getRequestURI())
                                                   .message(StringUtils.format("Validation on '%s' of object '%s' failed: %s", error.getField(),
                                                                               error.getObjectName(),
                                                                               error.getDefaultMessage()))
                                                   .exception(exception.getClass().getName())
                                                   .build();

        return toResponseEntity(response, HttpStatus.BAD_REQUEST.value(), errorResponse);
    }

    @ExceptionHandler({
        BadRequestException.class,
        InvalidExpressionException.class,
        ExpressionValidationException.class,
        HttpMessageNotReadableException.class,
        HttpRequestMethodNotSupportedException.class,
        HttpMediaTypeNotSupportedException.class,
        ServletRequestBindingException.class,
        NoResourceFoundException.class
    })
    public ResponseEntity<?> handleKnownExceptions(HttpServletRequest request, HttpServletResponse response, Exception exception) {
        ErrorResponse error = ErrorResponse.builder()
                                           .traceId((String) request.getAttribute("X-Bithon-TraceId"))
                                           .traceMode((String) request.getAttribute("X-Bithon-Trace-Mode"))
                                           .path(request.getRequestURI())
                                           .message(exception.getMessage())
                                           .exception(exception.getClass().getName())
                                           .build();
        return toResponseEntity(response, HttpStatus.BAD_REQUEST.value(), error);
    }

    @ExceptionHandler({Exception.class})
    public ResponseEntity<?> handleException(HttpServletRequest request,
                                             HttpServletResponse response,
                                             Exception exception) {
        int statusCode;
        HttpResponseMapping mapping = exception.getClass().getDeclaredAnnotation(HttpResponseMapping.class);
        if (mapping == null) {
            if (exception instanceof HttpMappableException) {
                statusCode = ((HttpMappableException) exception).getStatusCode();
            } else if (exception instanceof IOException) {
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

                if (!shouldSuppressExceptionLogging((IOException) exception)) {
                    // Logging unknown exception
                    log.error("Unexpected error", exception);
                }
            } else {
                statusCode = HttpStatus.INTERNAL_SERVER_ERROR.value();

                // Logging unknown exception
                log.error("Unexpected error", exception);
            }
        } else {
            statusCode = mapping.statusCode().value();
        }

        ErrorResponse error = ErrorResponse.builder().traceId((String) request.getAttribute("X-Bithon-TraceId"))
                                           .traceMode((String) request.getAttribute("X-Bithon-Trace-Mode"))
                                           .path(request.getRequestURI())
                                           .exception(exception.getClass().getName())
                                           .message(exception.getMessage())
                                           .build();

        return toResponseEntity(response, statusCode, error);
    }

    private ResponseEntity<?> toResponseEntity(HttpServletResponse response, int statusCode, ErrorResponse error) {
        if (MediaType.TEXT_EVENT_STREAM_VALUE.equals(response.getHeader("Content-Type"))) {
            try {
                String exceptionText = StringUtils.format("event: error\ndata: %s\n\n", objectMapper.writeValueAsString(error));
                return ResponseEntity.of(Optional.of(exceptionText));
            } catch (JsonProcessingException e) {
                return ResponseEntity.noContent().build();
            }
        } else {
            return ResponseEntity.status(statusCode)
                                 .body(error);
        }
    }

    private boolean shouldSuppressExceptionLogging(IOException exception) {
        if (!exception.getMessage().contains("Broken pipe")) {
            return false;
        }

        // If the exception is from the Spring Web Framework, usually this is due to connection close at the client side
        StackTraceElement[] stacks = exception.getStackTrace();
        for (int i = 0; i < stacks.length; i++) {
            if (stacks[i].getClassName().startsWith("org.springframework.web.context.request.")) {
                return true;
            }
        }

        return false;
    }
}
