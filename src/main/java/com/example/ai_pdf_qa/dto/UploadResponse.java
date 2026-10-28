package com.example.ai_pdf_qa.dto;
public record UploadResponse(
        String fileName,
        int totalPages,
        int totalCharacters,
        String  preview
){
}
