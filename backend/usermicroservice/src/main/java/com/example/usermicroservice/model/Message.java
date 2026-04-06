package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "messages")
public class Message {
    @Id
    private String id;
    private String senderId;
    private String receiverId;
    private String content;
    private LocalDateTime timestamp;
    private String status; // SENT, DELIVERED, READ
    private String attachmentId; // GridFS file ID if applicable
    private String fileName;
    private String fileType;
    private boolean deletedForSender = false;
    private boolean deletedForReceiver = false;
    private boolean isDeletedForEveryone = false;
    private boolean isEdited = false;

    public Message() {}

    public Message(String senderId, String receiverId, String content, String status) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
        this.timestamp = LocalDateTime.now();
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }

    public String getReceiverId() { return receiverId; }
    public void setReceiverId(String receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

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
    public void setEdited(boolean isEdited) { this.isEdited = isEdited; }
}
