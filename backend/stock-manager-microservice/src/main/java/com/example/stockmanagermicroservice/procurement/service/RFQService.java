package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.procurement.model.EquipmentRequest;
import com.example.stockmanagermicroservice.procurement.model.RFQ;
import com.example.stockmanagermicroservice.procurement.repository.EquipmentRequestRepository;
import com.example.stockmanagermicroservice.procurement.repository.RFQRepository;
import com.example.stockmanagermicroservice.procurement.model.RFQToken;
import com.example.stockmanagermicroservice.procurement.repository.RFQTokenRepository;
import com.example.stockmanagermicroservice.repository.SupplierRepository;
import com.example.stockmanagermicroservice.model.Supplier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class RFQService {

    @Autowired
    private RFQRepository rfqRepository;

    @Autowired
    private EquipmentRequestRepository requestRepository;

    @Autowired
    private EquipmentRequestService equipmentRequestService;

    @Autowired
    private PdfGenerationService pdfGenerationService;

    @Autowired
    private RFQTokenRepository rfqTokenRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@medinaflux.com}")
    private String fromEmail;

    @Value("${app.frontend.url:http://localhost:4200}")
    private String frontendUrl;

    /**
     * Creates an RFQ, generates PDF, sends emails to suppliers,
     * and updates the equipment request status to SENT_TO_SUPPLIERS.
     */
    public RFQ createAndSendRFQ(String requestId, List<String> supplierIds, List<String> supplierEmails, List<Integer> selectedItemIndices) throws Exception {
        EquipmentRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Equipment request not found: " + requestId));

        // Build and save the RFQ record first (to get an ID for the PDF filename)
        RFQ rfq = new RFQ();
        rfq.setRequestId(requestId);
        rfq.setSupplierIds(supplierIds);
        rfq.setSupplierEmails(supplierEmails);
        rfq.setSelectedItemIndices(selectedItemIndices);
        rfq.setCreatedAt(LocalDateTime.now());
        rfq.setUpdatedAt(LocalDateTime.now());
        rfq = rfqRepository.save(rfq); // save to get an ID

        // Generate RFQ PDF
        String pdfPath = pdfGenerationService.generateRFQPdf(request, rfq.getId(), selectedItemIndices);
        rfq.setPdfFilePath(pdfPath);
        rfq.setSentAt(LocalDateTime.now());
        rfq = rfqRepository.save(rfq);

        // Send emails with unique tokens
        if (mailSender != null && supplierIds != null && !supplierIds.isEmpty()) {
            sendRFQEmailsWithTokens(rfq, request, supplierIds, supplierEmails, pdfPath);
        }

        // Update request status
        equipmentRequestService.markAsSentToSuppliers(requestId);

        return rfq;
    }

    private void sendRFQEmailsWithTokens(RFQ rfq, EquipmentRequest request, List<String> supplierIds, List<String> supplierEmails, String pdfPath) {
        for (int i = 0; i < supplierIds.size(); i++) {
            String supplierId = supplierIds.get(i);
            String email = (supplierEmails != null && i < supplierEmails.size()) ? supplierEmails.get(i) : null;
            
            if (email == null || email.trim().isEmpty()) continue;

            try {
                // Generate secure token
                String tokenString = java.util.UUID.randomUUID().toString();
                RFQToken token = new RFQToken();
                token.setToken(tokenString);
                token.setRfqId(rfq.getId());
                token.setRequestId(request.getId());
                token.setSupplierId(supplierId);
                token.setSupplierEmail(email);
                
                // Fetch supplier name if possible
                Supplier supplier = supplierRepository.findById(supplierId).orElse(null);
                if (supplier != null) {
                    token.setSupplierName(supplier.getCompanyName());
                }
                
                token.setCreatedAt(LocalDateTime.now());
                token.setExpiresAt(LocalDateTime.now().plusDays(14)); // Valid for 14 days
                rfqTokenRepository.save(token);

                String magicLink = frontendUrl + "/supplier-respond/" + tokenString;

                MimeMessage message = mailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(message, true);
                helper.setFrom(fromEmail);
                helper.setTo(email);
                helper.setSubject("Request for Quotation — RFQ-" + rfq.getId().substring(0, 8).toUpperCase() + " | MedinaFlux");
                helper.setText(buildEmailBody(rfq, request, magicLink), true);
                helper.addAttachment("RFQ_" + rfq.getId().substring(0, 8).toUpperCase() + ".pdf",
                        new FileSystemResource(pdfPath));
                mailSender.send(message);
                System.out.println("✅ Successfully sent RFQ email to " + email);
            } catch (Exception e) {
                System.err.println("❌ Failed to send RFQ email to " + email + ": " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    private String buildEmailBody(RFQ rfq, EquipmentRequest request, String magicLink) {
        int itemCount = rfq.getSelectedItemIndices() != null ? rfq.getSelectedItemIndices().size() : 
                       (request.getItems() != null ? request.getItems().size() : 0);
                       
        return "<html><body style=\"font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; color: #334155; line-height: 1.6; background-color: #f1f5f9; padding: 20px;\">"
                + "<div style=\"max-width: 650px; margin: 0 auto; background: #ffffff; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);\">"
                + "<div style=\"background: linear-gradient(135deg, #3b82f6 0%, #1d4ed8 100%); color: white; padding: 32px 40px; text-align: center;\">"
                + "<h1 style=\"margin: 0; font-size: 28px; font-weight: 700; letter-spacing: 0.5px;\">MedinaFlux</h1>"
                + "<p style=\"margin: 8px 0 0; font-size: 16px; opacity: 0.9; text-transform: uppercase; letter-spacing: 1px;\">Official Request for Quotation</p>"
                + "</div>"
                + "<div style=\"padding: 40px;\">"
                + "<p style=\"font-size: 16px; margin-top: 0;\">Dear Valued Partner,</p>"
                + "<p style=\"font-size: 16px;\">MedinaFlux is currently sourcing IT equipment and we would like to invite you to participate in this procurement process. Please find attached our detailed <strong>Request for Quotation (RFQ-" 
                + rfq.getId().substring(0, 8).toUpperCase() + ")</strong>.</p>"
                
                + "<div style=\"background: #f8fafc; border-left: 4px solid #3b82f6; padding: 16px 24px; border-radius: 4px; margin: 24px 0;\">"
                + "<p style=\"margin: 0 0 8px; font-weight: 600; color: #0f172a;\">Quotation Requirements:</p>"
                + "<ul style=\"margin: 0; padding-left: 20px; color: #475569; font-size: 15px;\">"
                + "<li style=\"margin-bottom: 4px;\">Detailed unit pricing and total cost for <strong>" + itemCount + " requested item(s)</strong></li>"
                + "<li style=\"margin-bottom: 4px;\">Estimated delivery timelines</li>"
                + "<li style=\"margin-bottom: 4px;\">Payment terms and warranty details</li>"
                + "</ul>"
                + "</div>"
                
                + "<p style=\"font-size: 16px; margin-bottom: 24px;\">Please review the attached PDF specification document carefully. You can submit your quotation securely online, or reply directly to this email.</p>"
                
                + "<div style=\"text-align: center; margin: 32px 0;\">"
                + "<a href=\"" + magicLink + "\" style=\"background: #3b82f6; color: white; padding: 14px 28px; text-decoration: none; border-radius: 6px; font-weight: bold; font-size: 16px; display: inline-block;\">Submit Quotation Online</a>"
                + "</div>"
                
                + "<p style=\"font-size: 14px; color: #64748b; text-align: center;\">If the button doesn't work, copy and paste this link into your browser:<br>"
                + "<a href=\"" + magicLink + "\" style=\"color: #3b82f6;\">" + magicLink + "</a></p>"
                
                + "<div style=\"margin-top: 40px; padding-top: 24px; border-top: 1px solid #e2e8f0; font-size: 14px; color: #64748b;\">"
                + "<p style=\"margin: 0;\">Thank you for your time and continued partnership.</p>"
                + "<p style=\"margin: 8px 0 0;\">Best regards,</p>"
                + "<p style=\"margin: 4px 0 0; font-weight: 600; color: #334155;\">MedinaFlux Procurement & IT Management</p>"
                + "</div>"
                + "</div>"
                + "</div></body></html>";
    }

    public List<RFQ> getAllRFQs() {
        return rfqRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<RFQ> getRFQById(String id) {
        return rfqRepository.findById(id);
    }

    public Optional<RFQ> getRFQByRequestId(String requestId) {
        return rfqRepository.findByRequestId(requestId);
    }
}
