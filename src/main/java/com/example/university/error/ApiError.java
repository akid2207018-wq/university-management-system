package com.example.university.error;

import jakarta.servlet.http.HttpServletRequest;

import java.time.Instant;
import java.util.Map;

public record ApiError(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {

    public static ApiError of(HttpServletRequest request, int status, String error, String message) {
        return new ApiError(Instant.now(), status, error, message, request.getRequestURI(), null);
    }

    public static ApiError validation(HttpServletRequest request, Map<String, String> fieldErrors) {
        return new ApiError(
                Instant.now(),
                400,
                "Bad Request",
                "Request validation failed",
                request.getRequestURI(),
                fieldErrors
        );
    }
}
