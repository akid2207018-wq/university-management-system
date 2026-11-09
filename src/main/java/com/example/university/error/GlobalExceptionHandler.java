package com.example.university.error;

import com.example.university.auth.InvalidCredentialsException;
import com.example.university.user.InvalidRequestException;
import com.example.university.user.ResourceNotFoundException;
import com.example.university.user.UsernameAlreadyExistsException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            ResourceNotFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleUsernameConflict(
            UsernameAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler(InvalidRequestException.class)
    public ResponseEntity<ApiError> handleInvalidRequest(
            InvalidRequestException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.BAD_REQUEST, exception.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ApiError> handleInvalidCredentials(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.UNAUTHORIZED, exception.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidation(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        Map<String, String> fieldErrors = new LinkedHashMap<>();
        exception.getBindingResult().getFieldErrors().forEach(fieldError ->
                fieldErrors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage())
        );
        return ResponseEntity.badRequest().body(ApiError.validation(request, fieldErrors));
    }

    @ExceptionHandler({
            HttpMessageNotReadableException.class,
            MethodArgumentTypeMismatchException.class
    })
    public ResponseEntity<ApiError> handleMalformedRequest(Exception exception, HttpServletRequest request) {
        return error(request, HttpStatus.BAD_REQUEST, "Malformed or invalid request");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataConflict(
            DataIntegrityViolationException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.CONFLICT, "The request conflicts with existing data");
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiError> handleNoResource(
            NoResourceFoundException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.NOT_FOUND, "Resource was not found");
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiError> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not supported for this resource");
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ApiError> handleMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception,
            HttpServletRequest request
    ) {
        return error(request, HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Unhandled exception while processing {}", request.getRequestURI(), exception);
        return error(request, HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    private ResponseEntity<ApiError> error(HttpServletRequest request, HttpStatus status, String message) {
        return ResponseEntity.status(status)
                .body(ApiError.of(request, status.value(), status.getReasonPhrase(), message));
    }
}
