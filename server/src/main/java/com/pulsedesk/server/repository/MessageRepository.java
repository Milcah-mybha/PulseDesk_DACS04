package com.pulsedesk.server.repository;

import com.pulsedesk.server.model.MessageDocument;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends MongoRepository<MessageDocument, String> {

    List<MessageDocument> findByConversationIdOrderByTimestampAsc(String conversationId);

    List<MessageDocument> findByConversationIdOrderByTimestampDesc(String conversationId, Pageable pageable);

    long countByConversationId(String conversationId);

    void deleteByConversationId(String conversationId);
}
