package com.incode.verification.adapter.in.web;

import com.incode.verification.application.service.ProviderSubmissionException;
import com.incode.verification.application.service.VerificationConflictException;
import com.incode.verification.domain.valueobject.InvalidQueryException;
import java.net.URI;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    MethodArgumentTypeMismatchException.class,
    InvalidQueryException.class
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

  @ExceptionHandler(VerificationConflictException.class)
  ResponseEntity<ProblemDetail> conflict(VerificationConflictException exception) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    problem.setType(URI.create("https://www.incode.com/problems/verification-conflict"));
    problem.setTitle("Verification conflict");
    problem.setProperty("code", exception.code());
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .body(problem);
  }

  @ExceptionHandler(ProviderSubmissionException.class)
  ResponseEntity<ProblemDetail> provider(ProviderSubmissionException exception) {
    var problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatusCode.valueOf(exception.httpStatus()), exception.getMessage());
    problem.setType(URI.create("https://www.incode.com/problems/provider"));
    problem.setTitle(exception.title());
    problem.setProperty("code", exception.code());
    return ResponseEntity.status(exception.httpStatus())
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
