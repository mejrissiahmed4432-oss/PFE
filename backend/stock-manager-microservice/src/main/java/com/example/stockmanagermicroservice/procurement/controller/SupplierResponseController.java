package com.example.stockmanagermicroservice.procurement.controller;

import com.example.stockmanagermicroservice.procurement.model.SupplierResponse;
import com.example.stockmanagermicroservice.procurement.service.SupplierResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/procurement/responses")
@CrossOrigin(origins = "*")
public class SupplierResponseController {

    @Autowired
    private SupplierResponseService service;

    /**
     * POST /api/procurement/responses/upload
     * Multipart upload: PDF file + metadata as form fields
     */
    @PostMapping("/upload")
    public ResponseEntity<?> uploadResponse(
            @RequestParam("rfqId") String rfqId,
            @RequestParam("requestId") String requestId,
            @RequestParam("supplierId") String supplierId,
            @RequestParam("supplierName") String supplierName,
            @RequestParam(value = "totalPrice", required = false) Double totalPrice,
            @RequestParam(value = "deliveryDays", required = false) Integer deliveryDays,
            @RequestParam(value = "notes", required = false) String notes,
            @RequestParam(value = "currency", required = false) String currency,
            @RequestParam("file") MultipartFile file) {
        try {
            SupplierResponse response = service.uploadResponse(
                    rfqId, requestId, supplierId, supplierName,
                    totalPrice, deliveryDays, notes, currency, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** GET /api/procurement/responses/rfq/{rfqId} — All responses for an RFQ */
    @GetMapping("/rfq/{rfqId}")
    public ResponseEntity<List<SupplierResponse>> getByRfq(@PathVariable String rfqId) {
        return ResponseEntity.ok(service.getResponsesByRfq(rfqId));
    }

    /** GET /api/procurement/responses/request/{requestId} — All responses for a request */
    @GetMapping("/request/{requestId}")
    public ResponseEntity<List<SupplierResponse>> getByRequest(@PathVariable String requestId) {
        return ResponseEntity.ok(service.getResponsesByRequest(requestId));
    }

    /** GET /api/procurement/responses — All responses */
    @GetMapping
    public ResponseEntity<List<SupplierResponse>> getAll() {
        return ResponseEntity.ok(service.getAllResponses());
    }

    /** GET /api/procurement/responses/{id} — Single response */
    @GetMapping("/{id}")
    public ResponseEntity<SupplierResponse> getById(@PathVariable String id) {
        return service.getResponseById(id)
                .map(resp -> ResponseEntity.ok(resp))
                .orElse(ResponseEntity.<SupplierResponse>notFound().build());
    }

    /** PUT /api/procurement/responses/{id}/approve — IT Manager selects supplier */
    
    @PutMapping("/{id}/approve")
    public ResponseEntity<SupplierResponse> approve(@PathVariable String id) {
        return ResponseEntity.ok(service.approveResponse(id));
    }

    /** PUT /api/procurement/responses/{id}/reject — IT Manager rejects supplier */
    @PutMapping("/{id}/reject")
    public ResponseEntity<SupplierResponse> reject(@PathVariable String id) {
        return ResponseEntity.ok(service.rejectResponse(id));
    }

    /** GET /api/procurement/responses/{id}/download — Download supplier PDF */
    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadPdf(@PathVariable String id) {
        Optional<SupplierResponse> respOpt = service.getResponseById(id);
        if (respOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SupplierResponse resp = respOpt.get();
        File file = new File(resp.getPdfFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resp.getOriginalFileName() + "\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
    /** GET /api/procurement/responses/{id}/view — View supplier PDF in browser */
    @GetMapping("/{id}/view")
    public ResponseEntity<Resource> viewPdf(@PathVariable String id) {
        Optional<SupplierResponse> respOpt = service.getResponseById(id);
        if (respOpt.isEmpty() || respOpt.get().getPdfFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(respOpt.get().getPdfFilePath());
        if (!file.exists()) {
            System.err.println("[SupplierResponseController] File not found for view: " + file.getAbsolutePath());
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .body(resource);
    }
    @GetMapping("/favicon.ico")
    public ResponseEntity<Resource> getFavicon() {
        File file = new File("src/main/resources/static/favicon.ico");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, must-revalidate")
                .header(HttpHeaders.PRAGMA, "no-cache")
                .header(HttpHeaders.EXPIRES, "0")
                .contentType(MediaType.IMAGE_PNG)
                .body(resource);
    }
}
