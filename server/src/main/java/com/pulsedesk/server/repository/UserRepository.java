package com.pulsedesk.server.repository;

import com.pulsedesk.server.model.UserDocument;
import com.pulsedesk.server.model.UserRole;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends MongoRepository<UserDocument, String> {

    Optional<UserDocument> findByEmail(String email);

    Optional<UserDocument> findByStaffId(String staffId);

    Optional<UserDocument> findByEmailOrStaffId(String email, String staffId);

    boolean existsByEmail(String email);

    boolean existsByStaffId(String staffId);

    List<UserDocument> findByRoleAndActiveTrue(UserRole role);
}
