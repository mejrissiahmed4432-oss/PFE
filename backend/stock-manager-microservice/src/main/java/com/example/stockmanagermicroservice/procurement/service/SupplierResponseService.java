package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.procurement.model.SupplierResponse;
import com.example.stockmanagermicroservice.procurement.model.SupplierResponseStatus;
import com.example.stockmanagermicroservice.procurement.repository.SupplierResponseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SupplierResponseService {

    @Autowired
    private SupplierResponseRepository repository;

    @Autowired
    private EquipmentRequestService requestService;

    @Autowired
    private com.example.stockmanagermicroservice.service.NotificationService notificationService;

    @Value("${procurement.upload.dir:uploads/procurement}")
    private String uploadDir;

    /** Upload a supplier response PDF and store metadata */
    public SupplierResponse uploadResponse(
            String rfqId,
            String requestId,
            String supplierId,
            String supplierName,
            Double totalPrice,
            Integer deliveryDays,
            String notes,
            String currency,
            MultipartFile file) throws IOException {

        // Save the file
        String responseDir = uploadDir + "/responses";
        Files.createDirectories(Paths.get(responseDir));

        String fileExt = getExtension(file.getOriginalFilename());
        String savedFileName = "RESPONSE_" + rfqId.substring(0, Math.min(8, rfqId.length()))
                + "_" + UUID.randomUUID().toString().substring(0, 8) + fileExt;
        Path targetPath = Paths.get(responseDir, savedFileName);
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Build entity
        SupplierResponse response = new SupplierResponse();
        response.setRfqId(rfqId);
        response.setRequestId(requestId);
        response.setSupplierId(supplierId);
        response.setSupplierName(supplierName);
        response.setTotalPrice(totalPrice);
        response.setDeliveryDays(deliveryDays);
        response.setNotes(notes);
        response.setCurrency(currency != null ? currency : "TND");
        response.setPdfFilePath(targetPath.toString());
        response.setOriginalFileName(file.getOriginalFilename());
        response.setStatus(SupplierResponseStatus.PENDING);
        response.setCreatedAt(LocalDateTime.now());
        response.setUpdatedAt(LocalDateTime.now());

        SupplierResponse saved = repository.save(response);

        // Update parent request status → RESPONDED
        if (requestId != null) {
            requestService.markAsResponded(requestId);
            
            // Send real-time notification to managers
            notificationService.createNotification(
                "New Supplier Quotation",
                "Supplier " + supplierName + " has submitted a quotation for request " + requestId.substring(0, Math.min(8, requestId.length())),
                "SUCCESS",
                "PROCUREMENT",
                requestId,
                null, // For all
                "STOCK_MANAGER"
            );
            notificationService.createNotification(
                "New Supplier Quotation",
                "Supplier " + supplierName + " has submitted a quotation for request " + requestId.substring(0, Math.min(8, requestId.length())),
                "SUCCESS",
                "PROCUREMENT",
                requestId,
                null, // For all
                "IT_MANAGER"
            );
        }

        return saved;
    }

    /** IT Manager approves a supplier — marks others as rejected for the same RFQ */
    public SupplierResponse approveResponse(String id) {
        SupplierResponse winner = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Supplier response not found: " + id));

        // Reject all other responses for the same RFQ
        List<SupplierResponse> siblings = repository.findByRfqId(winner.getRfqId());
        for (SupplierResponse sibling : siblings) {
            if (!sibling.getId().equals(id)) {
                sibling.setStatus(SupplierResponseStatus.REJECTED_SUPPLIER);
                sibling.setUpdatedAt(LocalDateTime.now());
                repository.save(sibling);
            }
        }

        winner.setStatus(SupplierResponseStatus.APPROVED_SUPPLIER);
        winner.setUpdatedAt(LocalDateTime.now());
        return repository.save(winner);
    }

    /** IT Manager manually rejects a supplier response */
    public SupplierResponse rejectResponse(String id) {
        return repository.findById(id).map(resp -> {
            resp.setStatus(SupplierResponseStatus.REJECTED_SUPPLIER);
            resp.setUpdatedAt(LocalDateTime.now());
            return repository.save(resp);
        }).orElseThrow(() -> new RuntimeException("Supplier response not found: " + id));
    }

    public List<SupplierResponse> getResponsesByRfq(String rfqId) {
        return repository.findByRfqId(rfqId);
    }

    public List<SupplierResponse> getResponsesByRequest(String requestId) {
        return repository.findByRequestId(requestId);
    }

    public List<SupplierResponse> getAllResponses() {
        return repository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<SupplierResponse> getResponseById(String id) {
        return repository.findById(id);
    }

    public String getUploadDir() {
        return uploadDir;
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return ".pdf";
        return filename.substring(filename.lastIndexOf("."));
    }
}
