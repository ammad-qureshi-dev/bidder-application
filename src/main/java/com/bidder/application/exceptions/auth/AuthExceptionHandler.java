/* (C) 2026
bidder.app */
package com.bidder.application.exceptions.auth;

import com.bidder.application.controllers.message.ApiMessage;
import com.bidder.application.controllers.message.ApiResponse;
import com.bidder.application.controllers.message.ResponseType;
import java.util.List;
import java.util.NoSuchElementException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

// auth endpoints keep their own error responses (WARNING type, 401/403); ordered
// ahead of GlobalExceptionHandler, which handles everything else
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice(basePackages = "com.bidder.application.controllers.auth")
public class AuthExceptionHandler {

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ApiResponse<?>> handleIllegalState(
      IllegalStateException ex, WebRequest request) {
    return ResponseEntity.badRequest()
        .body(generateResponseBody(ex.getMessage(), ResponseType.WARNING));
  }

  @ExceptionHandler(NoSuchElementException.class)
  public ResponseEntity<ApiResponse<?>> handleNotFound(
      NoSuchElementException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(generateResponseBody(ex.getMessage(), ResponseType.WARNING));
  }

  @ExceptionHandler(BadCredentialsException.class)
  public ResponseEntity<ApiResponse<?>> handleAuthentication(
      BadCredentialsException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(generateResponseBody(ex.getMessage(), ResponseType.ERROR));
  }

  @ExceptionHandler(IllegalAccessException.class)
  public ResponseEntity<ApiResponse<?>> handleIllegalAccess(
      IllegalAccessException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(generateResponseBody(ex.getMessage(), ResponseType.ERROR));
  }

  @ExceptionHandler(RuntimeException.class)
  public ResponseEntity<ApiResponse<?>> handleGeneric(RuntimeException ex, WebRequest request) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(generateResponseBody(ex.getMessage(), ResponseType.ERROR));
  }

  private static ApiResponse<?> generateResponseBody(
      String exceptionMessage, ResponseType responseType) {
    var message = ApiMessage.builder().type(responseType).content(exceptionMessage).build();
    return ApiResponse.builder().data(null).messages(List.of(message)).build();
  }
}
