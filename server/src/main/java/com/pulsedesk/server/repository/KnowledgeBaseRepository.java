package com.pulsedesk.server.repository;

import com.pulsedesk.server.model.KnowledgeBaseDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KnowledgeBaseRepository extends MongoRepository<KnowledgeBaseDocument, String> {

    // Tìm kiếm toàn văn bản (Full-text search) có xếp hạng trọng số
    List<KnowledgeBaseDocument> findAllBy(TextCriteria criteria, Pageable pageable);

    List<KnowledgeBaseDocument> findByTagsContainingIgnoreCase(String tag);

    @Query("{ 'tags': { '$in': ?0 } }")
    List<KnowledgeBaseDocument> findByAnyTags(List<String> tags);

    List<KnowledgeBaseDocument> findAllByOrderByViewCountDesc(Pageable pageable);
}
