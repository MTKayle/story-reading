package org.example.storyreading.paymentservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler {

    private Map<String, Object> errorBody(HttpStatus status, String message, String path) {
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("status", status.value());
        body.put("error", status.getReasonPhrase());
        body.put("message", message);
        body.put("path", path);
        return body;
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Map<String, Object>> handleMissingHeader(MissingRequestHeaderException ex, HttpServletRequest request) {
        String msg = "Required header '" + ex.getHeaderName() + "' is not present.";
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody(HttpStatus.BAD_REQUEST, msg, request.getRequestURI()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleBadJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String errorMsg = ex.getMessage();
        // Log chi tiết để debug
        System.err.println("=== JSON Parse Error ===");
        System.err.println("Message: " + errorMsg);
        System.err.println("Request URI: " + request.getRequestURI());
        ex.printStackTrace();
        
        Map<String, Object> body = new HashMap<>();
        String userMessage = "Malformed JSON request. Please check your request format.";
        if (errorMsg != null && errorMsg.contains("BigDecimal")) {
            userMessage = "Giá truyện phải là số hợp lệ (ví dụ: 50000).";
        }
        body.put("error", userMessage);
        body.put("message", userMessage);
        body.put("details", errorMsg);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));
        String errorMessage = errors.isEmpty() ? "Validation failed" : errors;
        
        // Return simple error format that frontend expects
        Map<String, Object> body = new HashMap<>();
        body.put("error", errorMessage);
        body.put("message", errorMessage);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException.class)
    public ResponseEntity<Map<String, Object>> handleNotAcceptable(HttpMediaTypeNotAcceptableException ex, HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.NOT_ACCEPTABLE)
                .body(errorBody(HttpStatus.NOT_ACCEPTABLE, "Acceptable representations: [application/json, application/*+json].", request.getRequestURI()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleAny(Exception ex, HttpServletRequest request) {
        // Keep message short in production; include exception class for debugging locally
        String msg = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody(HttpStatus.INTERNAL_SERVER_ERROR, msg, request.getRequestURI()));
    }
}

