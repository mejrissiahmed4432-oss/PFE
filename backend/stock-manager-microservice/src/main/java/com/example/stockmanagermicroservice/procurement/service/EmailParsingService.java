package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.model.Supplier;
import com.example.stockmanagermicroservice.procurement.model.RFQ;
import com.example.stockmanagermicroservice.procurement.model.SupplierResponse;
import com.example.stockmanagermicroservice.procurement.repository.RFQRepository;
import com.example.stockmanagermicroservice.repository.SupplierRepository;
import jakarta.mail.*;
import jakarta.mail.internet.MimeMultipart;
import jakarta.mail.search.FlagTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class EmailParsingService {

    @Value("${spring.mail.username}")
    private String username;

    @Value("${spring.mail.password}")
    private String password;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private RFQRepository rfqRepository;

    @Autowired
    private SupplierResponseService responseService;

    // Run every 60 seconds
    @Scheduled(fixedDelay = 60000)
    public void scanInboxForQuotations() {
        if (username == null || password == null || username.isEmpty() || password.isEmpty()) {
            return; // Not configured
        }

        Properties properties = new Properties();
        properties.put("mail.store.protocol", "imaps");
        properties.put("mail.imaps.host", "imap.gmail.com");
        properties.put("mail.imaps.port", "993");
        properties.put("mail.imaps.ssl.enable", "true");

        try {
            Session session = Session.getInstance(properties, null);
            Store store = session.getStore("imaps");
            store.connect("imap.gmail.com", username, password);

            Folder inbox = store.getFolder("INBOX");
            inbox.open(Folder.READ_WRITE);

            // Find unread messages
            Message[] messages = inbox.search(new FlagTerm(new Flags(Flags.Flag.SEEN), false));

            for (Message message : messages) {
                try {
                    processMessage(message);
                    // Mark as read after processing
                    message.setFlag(Flags.Flag.SEEN, true);
                } catch (Exception e) {
                    System.err.println("❌ Failed to process email: " + e.getMessage());
                }
            }

            inbox.close(false);
            store.close();
        } catch (Exception e) {
            System.err.println("❌ IMAP Connection error: " + e.getMessage());
        }
    }

    private void processMessage(Message message) throws Exception {
        String subject = message.getSubject();
        if (subject == null) return;

        // Look for "RFQ-ABCDEFGH" or similar pattern
        Pattern pattern = Pattern.compile("RFQ-([A-Z0-9]{8})", Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(subject);

        if (!matcher.find()) {
            return; // Not an RFQ reply
        }

        String partialRfqId = matcher.group(1).toUpperCase();

        // Find the actual RFQ
        List<RFQ> rfqs = rfqRepository.findByIdStartingWithIgnoreCase(partialRfqId);
        if (rfqs.isEmpty()) return;
        RFQ rfq = rfqs.get(0);

        // Find the sender (Supplier)
        String from = message.getFrom()[0].toString();
        String supplierEmail = extractEmailAddress(from);

        Optional<Supplier> supplierOpt = supplierRepository.findByEmail(supplierEmail);
        if (supplierOpt.isEmpty()) return; // Unknown supplier
        Supplier supplier = supplierOpt.get();

        // Extract Text and Attachments
        String bodyText = "";
        MultipartFile pdfFile = null;

        if (message.getContent() instanceof Multipart) {
            Multipart multipart = (Multipart) message.getContent();
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);

                if (Part.ATTACHMENT.equalsIgnoreCase(bodyPart.getDisposition())) {
                    if (bodyPart.getFileName() != null && bodyPart.getFileName().toLowerCase().endsWith(".pdf")) {
                        byte[] content = bodyPart.getInputStream().readAllBytes();
                        pdfFile = new CustomMultipartFile("file", bodyPart.getFileName(), "application/pdf", content);
                    }
                } else if (bodyPart.isMimeType("text/plain")) {
                    bodyText = bodyPart.getContent().toString();
                } else if (bodyPart.getContent() instanceof Multipart) {
                    // Extract from nested multipart if necessary
                    bodyText += extractTextFromNestedMultipart((Multipart) bodyPart.getContent());
                }
            }
        } else if (message.isMimeType("text/plain")) {
            bodyText = message.getContent().toString();
        }

        if (pdfFile == null) {
            System.out.println("⚠️ Email from " + supplierEmail + " for " + partialRfqId + " has no PDF attachment.");
            return; 
        }

        // Very basic text parsing to try and find Price and Delivery (Optional)
        Double price = extractNumberFromText(bodyText, "Price:");
        Integer delivery = extractIntegerFromText(bodyText, "Delivery:");

        // Save Response using existing service
        SupplierResponse response = responseService.uploadResponse(
                rfq.getId(),
                rfq.getRequestId(),
                supplier.getId(),
                supplier.getCompanyName(),
                price,
                delivery,
                bodyText,
                "TND", // Default
                pdfFile
        );

        System.out.println("✅ Automatically ingested supplier response from " + supplierEmail);
    }

    private String extractTextFromNestedMultipart(Multipart multipart) throws Exception {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < multipart.getCount(); i++) {
            BodyPart bodyPart = multipart.getBodyPart(i);
            if (bodyPart.isMimeType("text/plain")) {
                result.append(bodyPart.getContent().toString());
            } else if (bodyPart.getContent() instanceof Multipart) {
                result.append(extractTextFromNestedMultipart((Multipart) bodyPart.getContent()));
            }
        }
        return result.toString();
    }

    private String extractEmailAddress(String from) {
        Pattern p = Pattern.compile("<(.*?)>");
        Matcher m = p.matcher(from);
        if (m.find()) {
            return m.group(1);
        }
        return from; // Fallback if no angle brackets
    }

    private Double extractNumberFromText(String text, String keyword) {
        Pattern p = Pattern.compile(keyword + "\\s*([0-9.,]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Double.parseDouble(m.group(1).replace(",", ""));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private Integer extractIntegerFromText(String text, String keyword) {
        Pattern p = Pattern.compile(keyword + "\\s*([0-9]+)", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    private static class CustomMultipartFile implements MultipartFile {
        private final String name;
        private final String originalFilename;
        private final String contentType;
        private final byte[] content;

        public CustomMultipartFile(String name, String originalFilename, String contentType, byte[] content) {
            this.name = name;
            this.originalFilename = originalFilename;
            this.contentType = contentType;
            this.content = content;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return originalFilename; }
        @Override public String getContentType() { return contentType; }
        @Override public boolean isEmpty() { return content == null || content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() throws IOException { return content; }
        @Override public InputStream getInputStream() throws IOException { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws IOException, IllegalStateException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}
