package com.example.stockmanagermicroservice.procurement.service;

import com.example.stockmanagermicroservice.procurement.model.EquipmentRequest;
import com.example.stockmanagermicroservice.procurement.model.EquipmentRequestItem;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PdfGenerationService {

    @Value("${procurement.upload.dir:uploads/procurement}")
    private String uploadDir;

    private static final DeviceRgb BRAND_BLUE   = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb LIGHT_BLUE   = new DeviceRgb(219, 234, 254);
    private static final DeviceRgb DARK_TEXT     = new DeviceRgb(15, 23, 42);
    private static final DeviceRgb SUBTLE_GRAY   = new DeviceRgb(248, 250, 252);
    private static final DeviceRgb BORDER_GRAY   = new DeviceRgb(226, 232, 240);

    /**
     * Generates a professional RFQ PDF document for the given equipment request.
     * Returns the path to the saved file.
     */
    public String generateRFQPdf(EquipmentRequest request, String rfqId, List<Integer> selectedItemIndices) throws Exception {
        // Ensure directory exists
        String rfqDir = uploadDir + "/rfq";
        Files.createDirectories(Paths.get(rfqDir));

        String fileName = "RFQ_" + rfqId + "_" + System.currentTimeMillis() + ".pdf";
        String filePath = rfqDir + "/" + fileName;

        PdfWriter writer = new PdfWriter(filePath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf, PageSize.A4);
        document.setMargins(40, 50, 40, 50);

        PdfFont boldFont   = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD);
        PdfFont normalFont = PdfFontFactory.createFont(com.itextpdf.io.font.constants.StandardFonts.HELVETICA);

        // ─── Header Banner ─────────────────────────────────────────────────
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        headerTable.setBorder(Border.NO_BORDER);

        Cell leftHeader = new Cell()
                .add(new Paragraph("MedinaFlux").setFont(boldFont).setFontSize(20).setFontColor(BRAND_BLUE))
                .add(new Paragraph("IT Management Platform").setFont(normalFont).setFontSize(9).setFontColor(new DeviceRgb(100, 116, 139)))
                .setBorder(Border.NO_BORDER);

        Cell rightHeader = new Cell()
                .add(new Paragraph("REQUEST FOR QUOTATION")
                        .setFont(boldFont).setFontSize(14).setFontColor(ColorConstants.WHITE)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("RFQ-" + rfqId.substring(0, Math.min(8, rfqId.length())).toUpperCase())
                        .setFont(boldFont).setFontSize(10).setFontColor(LIGHT_BLUE)
                        .setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(BRAND_BLUE)
                .setPadding(12)
                .setBorderRadius(new com.itextpdf.layout.properties.BorderRadius(6))
                .setBorder(Border.NO_BORDER);

        headerTable.addCell(leftHeader);
        headerTable.addCell(rightHeader);
        document.add(headerTable);
        document.add(new Paragraph("\n"));

        // ─── RFQ Meta Info ─────────────────────────────────────────────────
        Table metaTable = new Table(UnitValue.createPercentArray(new float[]{1, 1})).useAllAvailableWidth();
        metaTable.setBorder(Border.NO_BORDER);
        metaTable.setBackgroundColor(SUBTLE_GRAY);

        addMetaCell(metaTable, "Date Issued:", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMM yyyy")), boldFont, normalFont);
        addMetaCell(metaTable, "Request ID:", request.getId(), boldFont, normalFont);
        addMetaCell(metaTable, "Requested By:", request.getCreatedByName() != null ? request.getCreatedByName() : "Stock Manager", boldFont, normalFont);
        addMetaCell(metaTable, "Response Deadline:", LocalDateTime.now().plusDays(7).format(DateTimeFormatter.ofPattern("dd MMM yyyy")), boldFont, normalFont);
        document.add(metaTable);
        document.add(new Paragraph("\n"));

        // ─── Introduction Text ─────────────────────────────────────────────
        document.add(new Paragraph("Dear Supplier,")
                .setFont(boldFont).setFontSize(11).setFontColor(DARK_TEXT));
        document.add(new Paragraph(
                "We hereby invite you to submit your best quotation for the equipment listed below. " +
                "Please include unit prices, total cost, delivery timeframe, and payment terms. " +
                "Your response should be submitted as a PDF document.")
                .setFont(normalFont).setFontSize(10).setFontColor(new DeviceRgb(71, 85, 105)));
        document.add(new Paragraph("\n"));

        // ─── Items Table ───────────────────────────────────────────────────
        document.add(new Paragraph("Equipment Requirements")
                .setFont(boldFont).setFontSize(13).setFontColor(DARK_TEXT));
        document.add(new Paragraph("\n").setFontSize(4));

        float[] colWidths = {40f, 330f, 130f};
        Table itemTable = new Table(UnitValue.createPointArray(colWidths)).useAllAvailableWidth();

        // Header row
        String[] headers = {"#", "Equipment / Item Description & Specifications", "Quantity"};
        for (String h : headers) {
            itemTable.addHeaderCell(
                    new Cell().add(new Paragraph(h).setFont(boldFont).setFontSize(10).setFontColor(ColorConstants.WHITE))
                            .setBackgroundColor(BRAND_BLUE)
                            .setPadding(8)
                            .setBorder(Border.NO_BORDER)
            );
        }

        List<EquipmentRequestItem> allItems = request.getItems();
        List<EquipmentRequestItem> items = new java.util.ArrayList<>();
        
        if (selectedItemIndices != null && !selectedItemIndices.isEmpty()) {
            for (Integer index : selectedItemIndices) {
                if (index >= 0 && index < allItems.size()) {
                    items.add(allItems.get(index));
                }
            }
        } else {
            items.addAll(allItems);
        }

        for (int i = 0; i < items.size(); i++) {
            EquipmentRequestItem item = items.get(i);
            boolean isEven = i % 2 == 0;
            DeviceRgb rowBg = isEven ? new DeviceRgb(255, 255, 255) : SUBTLE_GRAY;

            StringBuilder desc = new StringBuilder(item.getName());
            if (item.getSelectedSpecs() != null && !item.getSelectedSpecs().isEmpty()) {
                item.getSelectedSpecs().forEach((k, v) -> desc.append("\n - ").append(k).append(": ").append(v));
            } else if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                desc.append("\n").append(item.getDescription());
            }

            itemTable.addCell(createDataCell(String.valueOf(i + 1), normalFont, rowBg));
            itemTable.addCell(createDataCell(desc.toString(), normalFont, rowBg));
            itemTable.addCell(createDataCell(String.valueOf(item.getQuantity()), normalFont, rowBg));
        }

        document.add(itemTable);
        document.add(new Paragraph("\n"));

        // ─── Notes ────────────────────────────────────────────────────────
        if (request.getNotes() != null && !request.getNotes().isEmpty()) {
            document.add(new Paragraph("Additional Notes:")
                    .setFont(boldFont).setFontSize(11).setFontColor(DARK_TEXT));
            document.add(new Paragraph(request.getNotes())
                    .setFont(normalFont).setFontSize(10).setFontColor(new DeviceRgb(71, 85, 105)));
            document.add(new Paragraph("\n"));
        }

        // ─── Terms ────────────────────────────────────────────────────────
        document.add(new Paragraph("Terms & Conditions")
                .setFont(boldFont).setFontSize(11).setFontColor(DARK_TEXT));
        String[] terms = {
                "1. Prices must include all applicable taxes and delivery costs.",
                "2. Quotation validity: minimum 30 days from submission date.",
                "3. Delivery timeline must be clearly stated.",
                "4. Payment terms must be specified (e.g., Net 30, Net 60).",
                "5. Please attach product datasheets or specifications where applicable."
        };
        for (String term : terms) {
            document.add(new Paragraph(term).setFont(normalFont).setFontSize(9)
                    .setFontColor(new DeviceRgb(71, 85, 105)).setMarginBottom(2));
        }

        document.add(new Paragraph("\n"));



        // ─── Footer ───────────────────────────────────────────────────────
        document.add(new Paragraph("\n"));
        document.add(new Paragraph("This document is confidential and intended solely for the named recipient.")
                .setFont(normalFont).setFontSize(8)
                .setFontColor(new DeviceRgb(148, 163, 184))
                .setTextAlignment(TextAlignment.CENTER)
                .setBorderTop(new SolidBorder(BORDER_GRAY, 1))
                .setPaddingTop(8));

        document.close();
        return filePath;
    }

    // ── Helper: Meta table cell ────────────────────────────────────────────
    private void addMetaCell(Table table, String label, String value, PdfFont boldFont, PdfFont normalFont) {
        Cell cell = new Cell()
                .add(new Paragraph(label).setFont(boldFont).setFontSize(9).setFontColor(new DeviceRgb(100, 116, 139)))
                .add(new Paragraph(value).setFont(boldFont).setFontSize(10).setFontColor(DARK_TEXT))
                .setPadding(10)
                .setBorder(new SolidBorder(BORDER_GRAY, 0.5f));
        table.addCell(cell);
    }

    // ── Helper: Data table cell ────────────────────────────────────────────
    private Cell createDataCell(String value, PdfFont font, DeviceRgb bgColor) {
        return new Cell()
                .add(new Paragraph(value).setFont(font).setFontSize(10).setFontColor(DARK_TEXT))
                .setBackgroundColor(bgColor)
                .setPadding(8)
                .setBorder(new SolidBorder(BORDER_GRAY, 0.5f));
    }
}
