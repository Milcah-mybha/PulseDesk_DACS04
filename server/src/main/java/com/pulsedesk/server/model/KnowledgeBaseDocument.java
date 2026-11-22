package com.pulsedesk.server.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.index.TextIndexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * MongoDB Document đại diện cho kho tài liệu tri thức (Knowledge Base)
 * phục vụ tìm kiếm thông tin và Retrieval-Augmented Generation (AI RAG).
 * 
 * Hỗ trợ:
 * - Tìm kiếm toàn văn bản (Full-Text Search) qua @TextIndexed
 * - Lưu trữ vector nhúng (embeddingVector) cho Vector Search / Cosine Similarity
 */
@Document(collection = "knowledge_base")
public class KnowledgeBaseDocument {

    @Id
    private String id;

    @TextIndexed(weight = 3.0f)
    private String title;

    @TextIndexed(weight = 1.0f)
    private String content;

    private String source; // Ví dụ: "Return Policy v2.1", "FAQ-Billing-2026", "Internal Guide"

    @Indexed
    private List<String> tags = new ArrayList<>();

    // Vector nhúng đại diện cho văn bản (ví dụ 768 hoặc 1536 chiều từ OpenAI / Gemini Text Embedding)
    private List<Double> embeddingVector = new ArrayList<>();

    private int viewCount = 0;

    private int helpfulCount = 0;

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public KnowledgeBaseDocument() {
    }

    public KnowledgeBaseDocument(String title, String content, String source, List<String> tags) {
        this.title = title;
        this.content = content;
        this.source = source;
        this.tags = tags != null ? tags : new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public List<Double> getEmbeddingVector() { return embeddingVector; }
    public void setEmbeddingVector(List<Double> embeddingVector) { this.embeddingVector = embeddingVector; }

    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }

    public int getHelpfulCount() { return helpfulCount; }
    public void setHelpfulCount(int helpfulCount) { this.helpfulCount = helpfulCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
