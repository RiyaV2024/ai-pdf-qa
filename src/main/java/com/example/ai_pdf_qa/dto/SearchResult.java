package com.example.ai_pdf_qa.dto;

public record SearchResult(
        String text,
        Object page,
        Object fileName
) {
}
