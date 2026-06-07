package com.example.aiservice.service;

import com.example.aiservice.model.ParsedItem;
import com.example.aiservice.model.QuotationAnalysisRequest;
import com.example.aiservice.model.QuotationAnalysisResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.model.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;

import java.util.ArrayList;
import java.util.List;

@Service
public class EquipmentParsingService {

    @Value("${spring.ai.openai.chat.options.model}")
    private String modelName;

    private static final Logger log = LoggerFactory.getLogger(EquipmentParsingService.class);

    private final ChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            You are a system that extracts equipment items and quantities from text.
            Respond ONLY with a JSON array of objects.
            Fields: "name" (string), "quantity" (number), "inferredCategory" (string), "detectedSpecs" (object with string keys and string values).
            Categories: Laptops, Peripherals, Network, Furniture, Office, Cables, Storage, RAM, Components.
            
            EXTREMELY IMPORTANT: Even if the user just types a noun phrase like "ram 16gb samsung" or "mouse", 
            treat it as 1 quantity of that item. Do NOT return an empty list if any hardware is mentioned.
            
            Extract any explicit specifications mentioned (e.g., size, speed, brand) into "detectedSpecs".
            If no equipment is found, return [].
            
            Example input: "ram 16gb samsung"
            Example output: [{"name": "ram", "quantity": 1, "inferredCategory": "RAM", "detectedSpecs": {"size": "16GB", "brand": "Samsung"}}]
            """;

    public EquipmentParsingService(ChatModel chatModel) {
        this.chatModel = chatModel;
        this.objectMapper = new ObjectMapper();
    }

    public List<ParsedItem> parseEquipmentRequest(String text) {
        try {
            Prompt prompt = new Prompt(List.of(
                    new SystemMessage(SYSTEM_PROMPT),
                    new UserMessage(text)
            ));

            String response = chatModel.call(prompt).getResult().getOutput().getText().trim();
            
            // Robust JSON extraction: Find the first [ and last ]
            int start = response.indexOf("[");
            int end = response.lastIndexOf("]");
            
            if (start == -1 || end == -1 || end < start) {
                log.warn("No JSON array found in AI response: {}", response);
                return new ArrayList<>();
            }
            
            String jsonPart = response.substring(start, end + 1);
            log.info("Extracted JSON for parsing: {}", jsonPart);
            
            return objectMapper.readValue(jsonPart, new TypeReference<List<ParsedItem>>() {});
            
        } catch (Exception e) {
            log.error("Failed to parse equipment request: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<String> suggestRelatedEquipment(String cartContext) {
        try {
            String suggestPrompt = """
                Given the following items currently in a user's procurement cart:
                %s
                
                Suggest 3-5 additional items or accessories that are typically bought together with these items.
                Respond ONLY with a JSON array of strings (e.g., ["Mouse", "HDMI Cable", "Docking Station"]).
                Do not include items that are already in the cart.
                """.formatted(cartContext);

            Prompt prompt = new Prompt(new UserMessage(suggestPrompt));
            String response = chatModel.call(prompt).getResult().getOutput().getText().trim();
            
            int start = response.indexOf("[");
            int end = response.lastIndexOf("]");
            
            if (start == -1 || end == -1 || end < start) {
                return new ArrayList<>();
            }
            
            String jsonPart = response.substring(start, end + 1);
            return objectMapper.readValue(jsonPart, new TypeReference<List<String>>() {});
            
        } catch (Exception e) {
            log.error("Failed to generate suggestions: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public List<String> autocompleteSpecsWithAI(String text) {
        try {
            String autocompletePrompt = """
                The user is typing an equipment request: "%s".
                Suggest 3 short continuations or specifications they might type next.
                For example, if they type "laptop ", suggest "16GB RAM", "Intel i7", "512GB SSD".
                If they type "monitor ", suggest "27 inch", "4k resolution".
                Respond ONLY with a JSON array of strings. Keep suggestions under 3 words each.
                """.formatted(text);

            Prompt prompt = new Prompt(new UserMessage(autocompletePrompt));
            String response = chatModel.call(prompt).getResult().getOutput().getText().trim();
            
            int start = response.indexOf("[");
            int end = response.lastIndexOf("]");
            
            if (start == -1 || end == -1 || end < start) {
                return new ArrayList<>();
            }
            
            String jsonPart = response.substring(start, end + 1);
            return objectMapper.readValue(jsonPart, new TypeReference<List<String>>() {});
            
        } catch (Exception e) {
            log.error("Failed to autocomplete specs: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    public QuotationAnalysisResponse compareQuotations(QuotationAnalysisRequest request) {
        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Analyze the following procurement request and the attached supplier invoice PDFs.\n\n");
            sb.append("Procurement Request:\n");
            sb.append("- Notes: ").append(request.getRequestNotes()).append("\n");
            sb.append("- Requested Items: ").append(request.getItems()).append("\n\n");
            
            sb.append("Supplier Quotes Summary (Metadata):\n");
            List<Message> messages = new ArrayList<>();
            
            for (QuotationAnalysisRequest.SupplierQuote quote : request.getQuotes()) {
                sb.append("- ").append(quote.getSupplierName())
                  .append(": ").append(quote.getTotalPrice()).append(" ").append(quote.getCurrency())
                  .append(", Delivery in ").append(quote.getDeliveryDays()).append(" days\n");
                  
                // If PDF is provided, attach it to the message
                if (quote.getPdfBase64() != null && !quote.getPdfBase64().isBlank()) {
                    try {
                        String base64Data = quote.getPdfBase64().contains(",") ? quote.getPdfBase64().split(",")[1] : quote.getPdfBase64();
                        byte[] pdfBytes = java.util.Base64.getDecoder().decode(base64Data);
                        Media pdfMedia = new Media(MimeTypeUtils.parseMimeType("application/pdf"), new ByteArrayResource(pdfBytes));
                        
                        // We add a specific instruction for this PDF
                        messages.add(new UserMessage("Document from " + quote.getSupplierName(), List.of(pdfMedia)));
                    } catch (Exception e) {
                        log.warn("Failed to decode PDF for supplier {}: {}", quote.getSupplierName(), e.getMessage());
                    }
                }
            }

            String comparePrompt = """
                SYSTEM INSTRUCTION:
                You are a procurement expert and document auditor. 
                1. Review the "Requested Items" vs what is actually written in the attached PDF invoices.
                2. Identify if any supplier is missing items or provided incorrect specifications (RAM, SSD, etc.) based on the PDFs.
                3. Recommend the best supplier based on price, delivery, and ACCURACY of their invoice.
                
                Respond ONLY with a JSON object:
                {
                  "recommendedSupplier": "Supplier Name",
                  "reasoning": "Explain WHY based on PDF content vs Request content. Mention if they matched the specs perfectly or not.",
                  "keyPros": ["Pro 1 (e.g. perfect spec match)", "Pro 2"],
                  "keyCons": ["Con 1 (e.g. missing 1 item in PDF)", "Con 2"],
                  "summary": "1-sentence executive summary"
                }
                """;

            messages.add(0, new SystemMessage(comparePrompt));
            messages.add(new UserMessage(sb.toString()));

            // Use configured model for multi-modal (PDF) support
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .model(modelName)
                    .build();

            Prompt prompt = new Prompt(messages, options);
            String response = chatModel.call(prompt).getResult().getOutput().getText().trim();
            
            int start = response.indexOf("{");
            int end = response.lastIndexOf("}");
            if (start == -1 || end == -1) return new QuotationAnalysisResponse();
            
            String jsonPart = response.substring(start, end + 1);
            return objectMapper.readValue(jsonPart, QuotationAnalysisResponse.class);
            
        } catch (Exception e) {
            log.error("Quotation comparison failed: {}", e.getMessage(), e);
            return new QuotationAnalysisResponse();
        }
    }
}
