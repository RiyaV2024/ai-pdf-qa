package com.example.ai_pdf_qa.service;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
@Service
public class PdfExtractionService {
    private void validate(MultipartFile file){
    if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File is Empty .");
        }
        String name = file.getOriginalFilename();
        if (name == null || !name.toLowerCase().endsWith(".pdf")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF file are allowed .");
        }
    }
    public List<Document> extract(MultipartFile file) {
        validate(file);
        try {
            final String fileName = file.getOriginalFilename();
            Resource resource = new ByteArrayResource(file.getBytes()) {
                @Override
                public String getFilename() {
                    return fileName;
                }
            };
            PagePdfDocumentReader reader = new PagePdfDocumentReader(resource);
            List<Document> cleaned = new ArrayList<>();
            for (Document p : reader.get()) {
                String text = p.getText().replaceAll("\\s+", " ").strip();
                cleaned.add(Document.builder().text(text).metadata(p.getMetadata()).build());
            }
            return cleaned;
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not read the PDF file.");
        }
    }
}