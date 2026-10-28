package com.example.ai_pdf_qa.dto;

import java.util.List;

public record AnswerResponse(
        String answer,
        List<SearchResult> sources
) {
}
