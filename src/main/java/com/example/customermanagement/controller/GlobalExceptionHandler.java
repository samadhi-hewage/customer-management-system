package com.example.customermanagement.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

// -------------------------------------------------------
// Catches exceptions thrown anywhere in the app and
// returns clean JSON error responses instead of ugly
// Spring stack traces.
// -------------------------------------------------------
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // -------------------------------------------------------
    // Handles @Valid validation failures
    // e.g. missing Name, NIC, or Date of Birth
    // Returns: { "name": "Name is mandatory", ... }
    // -------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, String> fieldErrors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String message   = error.getDefaultMessage();
            fieldErrors.put(fieldName, message);
        });

        Map<String, Object> response = new HashMap<>();
        response.put("status",    400);
        response.put("error",     "Validation Failed");
        response.put("fields",    fieldErrors);
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(response);
    }

    // -------------------------------------------------------
    // Handles business logic errors
    // e.g. "NIC number already exists" or "Customer not found"
    // -------------------------------------------------------
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(
            IllegalArgumentException ex) {

        log.warn("Business error: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("status",    400);
        response.put("error",     "Bad Request");
        response.put("message",   ex.getMessage());
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.badRequest().body(response);
    }

    // -------------------------------------------------------
    // Catches any unexpected errors so the app never
    // crashes with an unhandled 500 error
    // -------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(
            Exception ex) {

        log.error("Unexpected error: {}", ex.getMessage(), ex);

        Map<String, Object> response = new HashMap<>();
        response.put("status",    500);
        response.put("error",     "Internal Server Error");
        response.put("message",   "Something went wrong. Please try again.");
        response.put("timestamp", LocalDateTime.now().toString());

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}