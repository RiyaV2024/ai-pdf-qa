package com.example.ai_pdf_qa.dto;

public record ErrorResponse(
        int status,
        String message
) {
}
