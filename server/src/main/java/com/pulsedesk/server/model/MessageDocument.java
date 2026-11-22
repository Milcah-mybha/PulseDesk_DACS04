package com.pulsedesk.server.model;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.IndexDirection;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * MongoDB Document lưu trữ nội dung tin nhắn thời gian thực.
 * Đánh Compound Index (conversationId + timestamp giảm dần) phục vụ tải nhanh lịch sử chat.
 */
@Document(collection = "messages")
@CompoundIndexes({
    @CompoundIndex(name = "conv_time_idx", def = "{'conversationId': 1, 'timestamp': -1}")
})
public class MessageDocument {

    @Id
    private String id;

    @Indexed
    private String conversationId;

    private String senderId;

    private String senderName;

    private SenderType senderType;

    private String content;

    private FileAttachmentInfo attachment; // null nếu là tin nhắn văn bản thông thường

    @CreatedDate
    @Indexed(direction = IndexDirection.DESCENDING)
    private Instant timestamp = Instant.now();

    public MessageDocument() {
    }

    public MessageDocument(String conversationId, String senderId, String senderName,
                           SenderType senderType, String content, FileAttachmentInfo attachment) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.senderName = senderName;
        this.senderType = senderType;
        this.content = content;
        this.attachment = attachment;
        this.timestamp = Instant.now();
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getConversationId() { return conversationId; }
    public void setConversationId(String conversationId) { this.conversationId = conversationId; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }

    public SenderType getSenderType() { return senderType; }
    public void setSenderType(SenderType senderType) { this.senderType = senderType; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public FileAttachmentInfo getAttachment() { return attachment; }
    public void setAttachment(FileAttachmentInfo attachment) { this.attachment = attachment; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}
