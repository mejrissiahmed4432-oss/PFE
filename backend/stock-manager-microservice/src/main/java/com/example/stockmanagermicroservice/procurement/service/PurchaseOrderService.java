package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.procurement.model.PurchaseOrder;
import com.example.stockmanagermicroservice.procurement.model.RequestStatus;
import com.example.stockmanagermicroservice.procurement.model.SupplierResponse;
import com.example.stockmanagermicroservice.procurement.repository.PurchaseOrderRepository;
import com.example.stockmanagermicroservice.procurement.repository.SupplierResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.stockmanagermicroservice.service.NotificationService;
import com.example.stockmanagermicroservice.service.EquipmentService;
import com.example.stockmanagermicroservice.model.Equipment;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class PurchaseOrderService {

    @Autowired
    private PurchaseOrderRepository purchaseOrderRepository;

    @Autowired
    private SupplierResponseRepository supplierResponseRepository;

    @Autowired
    private EquipmentRequestService requestService;

    @Autowired
    private com.example.stockmanagermicroservice.service.EmailService emailService;

    @Autowired
    private com.example.stockmanagermicroservice.repository.SupplierRepository supplierRepository;

    @Autowired
    private NotificationService notificationService;

    /**
     * Creates a Purchase Order from the approved supplier response.
     * Also updates the equipment request to ORDER_CONFIRMED.
     */
    public PurchaseOrder createOrder(String requestId, String rfqId, String responseId) {
        SupplierResponse response = supplierResponseRepository.findById(responseId)
                .orElseThrow(() -> new RuntimeException("Supplier response not found: " + responseId));

        PurchaseOrder order = new PurchaseOrder();
        order.setRequestId(requestId);
        order.setRfqId(rfqId);
        order.setSelectedResponseId(responseId);
        order.setSupplierId(response.getSupplierId());
        order.setSupplierName(response.getSupplierName());
        order.setTotalPrice(response.getTotalPrice());
        order.setCurrency(response.getCurrency() != null ? response.getCurrency() : "TND");
        order.setDeliveryDays(response.getDeliveryDays());
        order.setStatus(RequestStatus.ORDER_CONFIRMED);
        order.setCreatedAt(LocalDateTime.now());
        order.setUpdatedAt(LocalDateTime.now());

        // Copy items from request
        requestService.getRequestById(requestId).ifPresent(req -> {
            order.setItems(req.getItems());
        });

        PurchaseOrder saved = purchaseOrderRepository.save(order);

        // Update the parent request to final status
        requestService.markAsOrderConfirmed(requestId, response.getSupplierName());

        // Send confirmation email to supplier
        supplierRepository.findById(response.getSupplierId()).ifPresent(supplier -> {
            emailService.sendOrderConfirmation(
                supplier.getEmail(),
                supplier.getCompanyName(),
                "PO-" + saved.getId().substring(0, 8).toUpperCase(),
                String.format("%.2f", saved.getTotalPrice()),
                saved.getCurrency()
            );
        });

        // Notify Stock Manager about confirmation
        requestService.getRequestById(requestId).ifPresent(req -> {
            notificationService.createNotification(
                "Order Confirmed",
                "Purchase order for " + req.getItems().size() + " items has been confirmed with " + saved.getSupplierName(),
                "SUCCESS",
                "PROCUREMENT",
                saved.getId(),
                req.getCreatedByUserId(),
                "STOCK_MANAGER"
            );
        });

        return saved;
    }

    public List<PurchaseOrder> getAllOrders() {
        return purchaseOrderRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<PurchaseOrder> getOrderById(String id) {
        return purchaseOrderRepository.findById(id);
    }

    public Optional<PurchaseOrder> getOrderByRequestId(String requestId) {
        return purchaseOrderRepository.findByRequestId(requestId);
    }

    @Autowired
    private EquipmentService equipmentService;

    /**
     * Stock Manager confirms physical receipt of the items.
     */
    public PurchaseOrder confirmReceipt(String orderId, String notes, Integer rating, boolean postToStock) {
        PurchaseOrder order = purchaseOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Purchase Order not found: " + orderId));

        order.setStatus(RequestStatus.RECEIVED);
        order.setReceivedAt(LocalDateTime.now());
        order.setReceiptNotes(notes);
        order.setSupplierRating(rating);
        order.setUpdatedAt(LocalDateTime.now());

        PurchaseOrder saved = purchaseOrderRepository.save(order);

        // Logic for AI Stock Entry
        if (postToStock && order.getItems() != null) {
            for (com.example.stockmanagermicroservice.procurement.model.EquipmentRequestItem item : order.getItems()) {
                int quantity = item.getQuantity();
                
                // For high-value equipment audit, we create individual records if quantity is small
                // or a single record with updated quantity if it's consumable/generic.
                // Here we'll create individual records as requested to allow unique serials/QR.
                for (int i = 0; i < quantity; i++) {
                    Equipment equipment = new Equipment();
                    equipment.setEquipmentName(item.getName());
                    equipment.setBrand(item.getSelectedSpecs() != null ? item.getSelectedSpecs().getOrDefault("Brand", "Standard") : "Standard");
                    equipment.setModel(item.getSelectedSpecs() != null ? item.getSelectedSpecs().getOrDefault("Model", "v1") : "v1");
                    
                    // AI Generated Serial Number
                    String sn = "SN-" + item.getName().substring(0, Math.min(3, item.getName().length())).toUpperCase() + 
                                "-" + System.currentTimeMillis() % 100000 + "-" + (i + 1);
                    equipment.setSerialNumber(sn);
                    
                    equipment.setCategory("Procured Equipment");
                    equipment.setType(item.getName());
                    equipment.setQte(1); // Individual unit tracking
                    equipment.setSupplier(order.getSupplierName());
                    equipment.setSupplierId(order.getSupplierId());
                    equipment.setStatus("Available");
                    equipment.setPurchaseDate(LocalDate.now());
                    equipment.setPurchasePrice(order.getTotalPrice() != null ? order.getTotalPrice() / quantity : 0.0);
                    equipment.setInvoiceRef("PO-" + orderId.substring(0, 8).toUpperCase());
                    
                    // Technical Specifications sync
                    if (item.getSelectedSpecs() != null) {
                        equipment.setSpecifications(new java.util.HashMap<>(item.getSelectedSpecs()));
                    }
                    
                    equipment.setCreatedBy("AI Procurement System");
                    equipment.setNote("Automatically added via GRN Audit #" + orderId.substring(0,8));
                    
                    // Explicitly generate QR code for audit compliance
                    equipment.setQrCode("QR-" + sn);
                    
                    equipmentService.createEquipment(equipment);
                }
            }
        }

        if (order.getRequestId() != null) {
            requestService.markAsReceived(order.getRequestId());
            
            // Notify IT Managers that items have arrived
            notificationService.createNotification(
                "Items Received" + (postToStock ? " & Invoiced to Stock" : ""),
                "Items for request " + order.getRequestId().substring(0,8) + " have been received and " + 
                (postToStock ? "automatically added to inventory by AI." : "marked as ready for manual entry."),
                "INFO",
                "PROCUREMENT",
                saved.getId(),
                null,
                "IT_MANAGER"
            );
        }

        return saved;
    }
}
