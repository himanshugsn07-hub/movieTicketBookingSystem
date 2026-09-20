package com.himanshu.movieTicketBookingSystem.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    public record ErrorResponse(int status, String error, String message, LocalDateTime timestamp) {
    }

    // Maps bad input to 400.
    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> validation(ValidationException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Maps missing show, seat, booking or catalog entries to 404.
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(NotFoundException e) {
        return build(HttpStatus.NOT_FOUND, e.getMessage());
    }

    // Maps access to someone else's booking to 403.
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> forbidden(UnauthorizedException e) {
        return build(HttpStatus.FORBIDDEN, e.getMessage());
    }

    // Maps conflicts, including unavailable seats, to 409.
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> conflict(ConflictException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    // Maps wrong status, started show or expired hold to 409.
    @ExceptionHandler(InvalidStateException.class)
    public ResponseEntity<ErrorResponse> invalidState(InvalidStateException e) {
        return build(HttpStatus.CONFLICT, e.getMessage());
    }

    // Maps any other booking system error to 400.
    @ExceptionHandler(BookingSystemException.class)
    public ResponseEntity<ErrorResponse> other(BookingSystemException e) {
        return build(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    // Maps failed request body validation to 400 listing the offending fields.
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> invalidBody(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(f -> f.getField() + " " + f.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return build(HttpStatus.BAD_REQUEST, message);
    }

    // Maps unreadable or malformed JSON to 400.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException e) {
        return build(HttpStatus.BAD_REQUEST, "Malformed or unreadable request body");
    }

    // Maps database constraint violations, such as deleting a city that has theatres, to 409.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> integrity(DataIntegrityViolationException e) {
        return build(HttpStatus.CONFLICT, "Operation violates a data constraint (item may still be in use)");
    }

    private ResponseEntity<ErrorResponse> build(HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(new ErrorResponse(status.value(), status.getReasonPhrase(), message, LocalDateTime.now()));
    }
}
