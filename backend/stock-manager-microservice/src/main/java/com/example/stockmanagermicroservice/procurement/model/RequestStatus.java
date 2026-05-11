package com.example.stockmanagermicroservice.procurement.model;

public enum RequestStatus {
    PENDING_IT_APPROVAL,
    APPROVED,
    REJECTED,
    SENT_TO_SUPPLIERS,
    RESPONDED,
    ORDER_CONFIRMED,
    RECEIVED
}
