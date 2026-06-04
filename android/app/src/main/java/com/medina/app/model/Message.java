package com.medina.app.model;

public class Message {
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private String timestamp; // Raw ISO timestamp string from backend
    private String status; // SENT, DELIVERED, READ
    private String attachmentId;
    private String fileName;
    private String fileType;
    private boolean deletedForSender;
    private boolean deletedForReceiver;
    private boolean isDeletedForEveryone;
    private boolean isEdited;

    private boolean isOwn;

    // Additional fields for file attachments
    private String attachmentName; // Display name for the attached file
    private String attachmentUrl;  // URL returned by the upload endpoint

    // Populated client-side from the user list for display in bubbles
    private transient String senderName;

    public Message() {}

    public Message(String senderId, String receiverId, String content, String status) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.status = status;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getAttachmentId() { return attachmentId; }
    public void setAttachmentId(String attachmentId) { this.attachmentId = attachmentId; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public boolean isDeletedForSender() { return deletedForSender; }
    public void setDeletedForSender(boolean deletedForSender) { this.deletedForSender = deletedForSender; }

    public boolean isDeletedForReceiver() { return deletedForReceiver; }
    public void setDeletedForReceiver(boolean deletedForReceiver) { this.deletedForReceiver = deletedForReceiver; }

    public boolean isDeletedForEveryone() { return isDeletedForEveryone; }
    public void setDeletedForEveryone(boolean deletedForEveryone) { isDeletedForEveryone = deletedForEveryone; }

    public boolean isEdited() { return isEdited; }
    public void setEdited(boolean edited) { isEdited = edited; }

    public boolean isOwn() { return isOwn; }
    public void setOwn(boolean own) { isOwn = own; }

    public String getAttachmentName() { return attachmentName; }
    public void setAttachmentName(String attachmentName) { this.attachmentName = attachmentName; }

    public String getAttachmentUrl() { return attachmentUrl; }
    public void setAttachmentUrl(String attachmentUrl) { this.attachmentUrl = attachmentUrl; }

    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
}
