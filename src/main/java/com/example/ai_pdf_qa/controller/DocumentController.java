package com.example.ai_pdf_qa.controller;
import com.example.ai_pdf_qa.dto.AnswerResponse;
import com.example.ai_pdf_qa.dto.DocumentInfo;
import com.example.ai_pdf_qa.dto.SearchResult;
import com.example.ai_pdf_qa.service.DocumentService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import com.example.ai_pdf_qa.dto.UploadResponse;
import com.example.ai_pdf_qa.service.PdfExtractionService;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {
    private final PdfExtractionService pdfExtractionService;
    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final DocumentService documentService;
    public DocumentController(PdfExtractionService pdfExtractionService, VectorStore vectorStore, ChatClient.Builder chatClientBuilder, DocumentService documentService) {
        this.pdfExtractionService = pdfExtractionService;
        this.vectorStore = vectorStore;
        this.chatClient = chatClientBuilder.build();
        this.documentService = documentService;
    }

    @PostMapping("/upload")
    public UploadResponse upload(@RequestParam("file") MultipartFile file) {
        List<Document> pages = pdfExtractionService.extract(file);
        TokenTextSplitter splitter = new TokenTextSplitter();
        List<Document> chunks = splitter.apply(pages);
        String fileName = file.getOriginalFilename();
        Filter.Expression sameFile = new FilterExpressionBuilder()
                .eq("file_name", fileName)
                .build();
        vectorStore.delete(sameFile);        vectorStore.add(chunks);
        StringBuilder sb = new StringBuilder();
        for (Document page : pages) {
            sb.append(page.getText()).append("\n");
        }
        String fullText = sb.toString().replaceAll("\\s+", " ");
        String preview = fullText.strip();
        if (preview.length() > 300) {
            preview = preview.substring(0, 300);
        }

        return new UploadResponse(
                file.getOriginalFilename(),
                pages.size(),
                fullText.length(),
                preview
        );
    }

    @GetMapping("/search")
    public List<SearchResult> search(@RequestParam("question") String question) {
        List<Document> found = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(4)
                        .build()
        );
        List<SearchResult> results = new ArrayList<>();
        for (Document d : found) {
            results.add(new SearchResult(
                    d.getText(),
                    d.getMetadata().get("page_number"),
                    d.getMetadata().get("file_name")
            ));
        }
        return results;
    }

    @GetMapping("/ask")
    public AnswerResponse ask(@RequestParam("question") String question) {
        List<Document> found = vectorStore.similaritySearch(
                SearchRequest.builder()
                        .query(question)
                        .topK(4)
                        .similarityThreshold(0.65)

                        .build()
        );

        if (found.isEmpty()) {
            return new AnswerResponse("Mujhe PDF mein ye nahi mila.", new ArrayList<>());
        }
        StringBuilder context = new StringBuilder();
        List<SearchResult> sources = new ArrayList<>();
        for (Document d : found) {
            context.append(d.getText()).append("\n\n");          // KHAALI 1
            sources.add(new SearchResult(
                    d.getText(),
                    d.getMetadata().get("page_number"),
                    d.getMetadata().get("file_name")
            ));
        }

        String answer = chatClient.prompt()
                .user(u -> u.text("""
                                You are a helpful assistant. Answer the QUESTION using only the CONTEXT below.
                                Rules:
                                Rules:
                                - Write 2-3 simple sentences in Hinglish (Hindi written in English letters).
                                - Explain in your own words, do not copy the context.
                                - Use the reply "Mujhe PDF mein ye nahi mila." ONLY when the CONTEXT has nothing about the question. If you answered, never add it.
                                CONTEXT:
                                {context}
                                
                                QUESTION:
                                {question}
                                
                                
                                ANSWER:
                                """)
                        .param("context", context.toString())
                        .param("question", question))
                .call()
                .content();
        return new AnswerResponse(answer, sources);
    }
    @GetMapping
    public List<DocumentInfo> list() {
        return documentService.list();    }
    @DeleteMapping("/{fileName}")
    public String delete(@PathVariable String fileName) {
        if (!documentService.exists(fileName)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Ye PDF nahi mili: " + fileName);
        }
        documentService.deleteByFileName(fileName);
        return "Deleted: " + fileName;
    }


}
