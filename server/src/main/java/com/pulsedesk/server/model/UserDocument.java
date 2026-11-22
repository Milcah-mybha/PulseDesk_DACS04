package com.pulsedesk.server.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Document đại diện cho người dùng (Nhân viên, Khách hàng, Quản trị viên).
 * Lưu mật khẩu đã được băm (BCrypt), tuyệt đối không lưu plaintext.
 */
@Document(collection = "users")
public class UserDocument {

    @Id
    private String id;

    @Indexed(unique = true)
    private String email;

    @Indexed
    private String staffId; // Ví dụ: "staff_01", "admin_01" (null đối với khách hàng)

    private String fullName;

    // Không bao giờ serialize ra JSON trả về client
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String passwordHash;

    private UserRole role = UserRole.CUSTOMER;

    private boolean active = true;

    private String avatarUrl;

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    private Instant updatedAt = Instant.now();

    public UserDocument() {
    }

    public UserDocument(String email, String staffId, String fullName, String passwordHash, UserRole role) {
        this.email = email;
        this.staffId = staffId;
        this.fullName = fullName;
        this.passwordHash = passwordHash;
        this.role = role;
        this.active = true;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStaffId() { return staffId; }
    public void setStaffId(String staffId) { this.staffId = staffId; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
