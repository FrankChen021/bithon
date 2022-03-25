package org.bithon.server.starter.exception;

import lombok.Builder;
import lombok.Data;
import org.bithon.component.brpc.exception.BadRequestException;
import org.bithon.component.commons.utils.Preconditions;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Frank Chen
 * @date 24/3/22 3:05 PM
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler({BadRequestException.class, Preconditions.InvalidValueException.class})
    public ResponseEntity<ErrorResponse> handleException(HttpServletRequest request, RuntimeException exception) {
        return ResponseEntity.badRequest().body(ErrorResponse.builder()
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
