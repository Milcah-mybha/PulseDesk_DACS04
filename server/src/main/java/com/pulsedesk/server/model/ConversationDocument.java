package com.pulsedesk.server.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Document quản lý thông tin phiên hội thoại / ticket tư vấn CSKH.
 * Đánh chỉ mục (Compound Indexes) phục vụ lọc nhanh theo nhân viên phụ trách và trạng thái.
 */
@Document(collection = "conversations")
@CompoundIndexes({
    @CompoundIndex(name = "staff_status_idx", def = "{'assignedStaffId': 1, 'status': 1}"),
    @CompoundIndex(name = "customer_updated_idx", def = "{'customerId': 1, 'updatedAt': -1}")
})
public class ConversationDocument {

    @Id
    private String id;

    @Indexed
    private String customerId;

    private String customerName;

    private String customerEmail;

    @Indexed
    private String assignedStaffId;

    private String assignedStaffName;

    private ConversationStatus status = ConversationStatus.ACTIVE;

    private String issueType; // Ví dụ: "Billing Issue #9876", "Product Return Request"

    private String lastMessageSnippet;

    private Instant lastMessageAt;

    private int unreadCount = 0;

    @CreatedDate
    private Instant createdAt = Instant.now();

    @LastModifiedDate
    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant updatedAt = Instant.now();

    public ConversationDocument() {
    }

    public ConversationDocument(String customerId, String customerName, String customerEmail,
                                String assignedStaffId, String assignedStaffName, String issueType) {
        this.customerId = customerId;
        this.customerName = customerName;
        this.customerEmail = customerEmail;
        this.assignedStaffId = assignedStaffId;
        this.assignedStaffName = assignedStaffName;
        this.issueType = issueType;
        this.status = ConversationStatus.ACTIVE;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public String getAssignedStaffId() { return assignedStaffId; }
    public void setAssignedStaffId(String assignedStaffId) { this.assignedStaffId = assignedStaffId; }

    public String getAssignedStaffName() { return assignedStaffName; }
    public void setAssignedStaffName(String assignedStaffName) { this.assignedStaffName = assignedStaffName; }

    public ConversationStatus getStatus() { return status; }
    public void setStatus(ConversationStatus status) { this.status = status; }

    public String getIssueType() { return issueType; }
    public void setIssueType(String issueType) { this.issueType = issueType; }

    public String getLastMessageSnippet() { return lastMessageSnippet; }
    public void setLastMessageSnippet(String lastMessageSnippet) { this.lastMessageSnippet = lastMessageSnippet; }

    public Instant getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(Instant lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public int getUnreadCount() { return unreadCount; }
    public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
