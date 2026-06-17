package com.example.usermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;
import java.util.List;

@Document(collection = "personal_requests")
public class PersonalRequest {
    @Id
    private String id;
    
    private String userId;
    private String userName;
    
    private List<RequestedItem> requestedItems;
    
    private String reason;
    
    private String status; // PENDING, APPROVED, REJECTED
    private String itManagerNote;
    
    @CreatedDate
    private LocalDateTime createdAt;
    
    @LastModifiedDate
    private LocalDateTime updatedAt;
    
    private LocalDateTime reviewedAt;

    public PersonalRequest() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }

    public List<RequestedItem> getRequestedItems() { return requestedItems; }
    public void setRequestedItems(List<RequestedItem> requestedItems) { this.requestedItems = requestedItems; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getItManagerNote() { return itManagerNote; }
    public void setItManagerNote(String itManagerNote) { this.itManagerNote = itManagerNote; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public LocalDateTime getReviewedAt() { return reviewedAt; }
    public void setReviewedAt(LocalDateTime reviewedAt) { this.reviewedAt = reviewedAt; }

    public static class RequestedItem {
        private String itemId;
        private String itemType; // EQUIPMENT or SOFTWARE
        private String itemName;
        private String brand;
        private String model;
        private String type;
        private String version;

        public RequestedItem() {}

        public String getItemId() { return itemId; }
        public void setItemId(String itemId) { this.itemId = itemId; }

        public String getItemType() { return itemType; }
        public void setItemType(String itemType) { this.itemType = itemType; }

        public String getItemName() { return itemName; }
        public void setItemName(String itemName) { this.itemName = itemName; }

        public String getBrand() { return brand; }
        public void setBrand(String brand) { this.brand = brand; }

        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getVersion() { return version; }
        public void setVersion(String version) { this.version = version; }
    }
}
