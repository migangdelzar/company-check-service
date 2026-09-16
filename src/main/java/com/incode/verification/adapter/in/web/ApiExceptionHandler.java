package com.incode.verification.adapter.in.web;

import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class
  })
  ResponseEntity<ProblemDetail> invalid(Exception exception) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail(exception));
    problem.setType(URI.create("https://example.com/problems/invalid-request"));
    problem.setTitle("Invalid request");
    return ResponseEntity.badRequest()
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<ProblemDetail> notFound(IllegalArgumentException exception) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    problem.setType(URI.create("https://example.com/problems/verification-not-found"));
    problem.setTitle("Verification not found");
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  private String detail(Exception exception) {
    if (exception instanceof MethodArgumentNotValidException invalid) {
      return invalid.getBindingResult().getFieldErrors().stream()
          .map(error -> error.getField() + ": " + error.getDefaultMessage())
          .collect(Collectors.joining(", "));
    }
    return "Request parameters are invalid";
  }
}
