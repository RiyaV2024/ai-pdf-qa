package com.example.ai_pdf_qa.service;

import com.example.ai_pdf_qa.dto.DocumentInfo;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DocumentService {

    private final JdbcTemplate jdbcTemplate;
    private final VectorStore vectorStore;
    public DocumentService(JdbcTemplate jdbcTemplate, VectorStore vectorStore) {
        this.jdbcTemplate = jdbcTemplate;
        this.vectorStore = vectorStore;
    }

    public List<DocumentInfo> list() {
        String sql = "SELECT metadata->>'file_name' AS file_name, COUNT(*) AS chunks "
                + "FROM vector_store GROUP BY 1";

        return jdbcTemplate.query(sql, (rs, i) -> new DocumentInfo(
                rs.getString("file_name"),
                rs.getInt("chunks")
        ));
    }

    public boolean exists(String fileName) {
        String sql = "SELECT COUNT(*) FROM vector_store WHERE metadata->>'file_name' = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, fileName);
        return count != null && count > 0;
    }
    public void deleteByFileName(String fileName) {
        Filter.Expression sameFile = new FilterExpressionBuilder()
                .eq("file_name", fileName)          // KHAALI
                .build();
        vectorStore.delete(sameFile);
    }
}