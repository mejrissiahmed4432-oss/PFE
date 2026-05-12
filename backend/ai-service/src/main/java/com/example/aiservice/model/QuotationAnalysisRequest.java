package com.example.aiservice.model;

import java.util.List;
import java.util.Map;

public class QuotationAnalysisRequest {
    private String requestNotes;
    private List<Map<String, Object>> items;
    private List<SupplierQuote> quotes;

    public QuotationAnalysisRequest() {}

    public String getRequestNotes() { return requestNotes; }
    public void setRequestNotes(String requestNotes) { this.requestNotes = requestNotes; }

    public List<Map<String, Object>> getItems() { return items; }
    public void setItems(List<Map<String, Object>> items) { this.items = items; }

    public List<SupplierQuote> getQuotes() { return quotes; }
    public void setQuotes(List<SupplierQuote> quotes) { this.quotes = quotes; }

    public static class SupplierQuote {
        private String supplierName;
        private Double totalPrice;
        private Integer deliveryDays;
        private String currency;
        private String pdfBase64; // Base64 encoded PDF content

        public String getSupplierName() { return supplierName; }
        public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

        public Double getTotalPrice() { return totalPrice; }
        public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

        public Integer getDeliveryDays() { return deliveryDays; }
        public void setDeliveryDays(Integer deliveryDays) { this.deliveryDays = deliveryDays; }

        public String getCurrency() { return currency; }
        public void setCurrency(String currency) { this.currency = currency; }

        public String getPdfBase64() { return pdfBase64; }
        public void setPdfBase64(String pdfBase64) { this.pdfBase64 = pdfBase64; }
    }
}
   