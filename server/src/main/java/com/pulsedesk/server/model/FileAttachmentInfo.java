package com.pulsedesk.server.model;

/**
 * Thông tin tệp đính kèm (PDF, tài liệu hóa đơn, hình ảnh) kèm mã kiểm tra toàn vẹn SHA-256.
 */
public class FileAttachmentInfo {

    private String fileName;
    private String fileType; // Ví dụ: "application/pdf", "image/png"
    private long fileSizeBytes;
    private String fileUrl;
    private String sha256; // Mã hash SHA-256 phục vụ xác thực toàn vẹn (integrity verification)

    public FileAttachmentInfo() {
    }

    public FileAttachmentInfo(String fileName, String fileType, long fileSizeBytes, String fileUrl, String sha256) {
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSizeBytes = fileSizeBytes;
        this.fileUrl = fileUrl;
        this.sha256 = sha256;
    }

    // Getters and Setters
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSizeBytes() { return fileSizeBytes; }
    public void setFileSizeBytes(long fileSizeBytes) { this.fileSizeBytes = fileSizeBytes; }

    public String getFileUrl() { return fileUrl; }
    public void setFileUrl(String fileUrl) { this.fileUrl = fileUrl; }

    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
}
