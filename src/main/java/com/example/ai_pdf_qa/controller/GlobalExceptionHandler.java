package com.example.ai_pdf_qa.controller;

import org.springframework.http.ResponseEntity;
import com.example.ai_pdf_qa.dto.ErrorResponse;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleStatus (ResponseStatusException ex){
        int status = ex.getStatusCode().value();
        String message = ex.getReason();        return ResponseEntity.status(status).body(new ErrorResponse(status, message));
    }
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex) {
        return ResponseEntity.status(500)
                .body(new ErrorResponse(500, "Kuch galat ho gaya. Thodi der baad dobara try karo."));
    }
}
