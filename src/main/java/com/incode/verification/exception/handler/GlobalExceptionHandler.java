package com.incode.verification.exception.handler;

import com.incode.verification.exception.base.BusinessException;
import com.incode.verification.exception.domain.InvalidQueryException;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.stream.Collectors;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final URI PROBLEM_TYPE =
      URI.create("https://www.incode.com/problems/verification");

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    HandlerMethodValidationException.class,
    ConstraintViolationException.class,
    InvalidQueryException.class
  })
  ProblemDetail invalid(Exception exception) {
    return problem(HttpStatus.BAD_REQUEST, "Invalid request", detail(exception), null);
  }

  @ExceptionHandler(BusinessException.class)
  ProblemDetail business(BusinessException exception) {
    return problem(
        HttpStatusCode.valueOf(exception.status()),
        exception.title(),
        exception.getMessage(),
        exception.code());
  }

  @ExceptionHandler(Exception.class)
  ProblemDetail unexpected(Exception exception) {
    log.error("Unexpected verification API failure", exception);
    return problem(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "Internal server error",
        "The verification service could not complete the request",
        null);
  }

  private static ProblemDetail problem(
      HttpStatusCode status, String title, @Nullable String detail, @Nullable String code) {
    var problem = ProblemDetail.forStatusAndDetail(status, detail);
    problem.setType(PROBLEM_TYPE);
    problem.setTitle(title);
    if (code == null) {
      return problem;
    }
    problem.setProperty("code", code);
    return problem;
  }

  private static String detail(Exception exception) {
    if (exception instanceof MethodArgumentNotValidException invalid) {
      return invalid.getBindingResult().getFieldErrors().stream()
          .map(error -> error.getField() + ": " + error.getDefaultMessage())
          .collect(Collectors.joining(", "));
    }
    if (exception instanceof ConstraintViolationException violations) {
      return violations.getConstraintViolations().stream()
          .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
          .collect(Collectors.joining(", "));
    }
    return "Request parameters are invalid";
  }
}
