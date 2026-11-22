package com.pulsedesk.server.repository;

import com.pulsedesk.server.model.ConversationDocument;
import com.pulsedesk.server.model.ConversationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends MongoRepository<ConversationDocument, String> {

    List<ConversationDocument> findByAssignedStaffIdOrderByUpdatedAtDesc(String staffId);

    List<ConversationDocument> findByCustomerIdOrderByUpdatedAtDesc(String customerId);

    List<ConversationDocument> findByStatusOrderByUpdatedAtDesc(ConversationStatus status);

    List<ConversationDocument> findByAssignedStaffIdAndStatus(String staffId, ConversationStatus status);

    @Query("{ '$or': [ { 'assignedStaffId': ?0 }, { 'assignedStaffId': null } ], 'status': { '$in': ['ACTIVE', 'PENDING'] } }")
    List<ConversationDocument> findActiveTicketsForStaff(String staffId);

    List<ConversationDocument> findAllByOrderByUpdatedAtDesc(Pageable pageable);
}
