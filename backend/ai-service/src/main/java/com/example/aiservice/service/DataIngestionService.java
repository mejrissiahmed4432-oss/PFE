package com.example.aiservice.service;

import com.example.aiservice.clients.StockClient;
import com.example.aiservice.clients.TechnicianClient;
import com.example.aiservice.clients.UserClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Scheduled service that ingests highly detailed data into the vector store.
 * Ensures the AI "knows" every attribute of every entity.
 */
@Service
public class DataIngestionService {

    private static final Logger log = LoggerFactory.getLogger(DataIngestionService.class);

    private final StockClient stockClient;
    private final TechnicianClient technicianClient;
    private final UserClient userClient;
    private final VectorStoreService vectorStore;

    @Value("${app.ai.reindex.on-startup:true}")
    private boolean reindexOnStartup;

    public DataIngestionService(StockClient stockClient,
                                TechnicianClient technicianClient,
                                UserClient userClient,
                                VectorStoreService vectorStore) {
        this.stockClient = stockClient;
        this.technicianClient = technicianClient;
        this.userClient = userClient;
        this.vectorStore = vectorStore;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (reindexOnStartup) reindex();
    }

    @Scheduled(fixedDelayString = "PT6H", initialDelayString = "PT6H")
    public void scheduledReindex() {
        reindex();
    }

    public Map<String, Object> reindex() {
        vectorStore.clearAll();
        int s = indexStockData();
        int t = indexTechData();
        int u = indexUserData();
        log.info("Indexed {} Stock, {} Tech, {} User docs", s, t, u);
        return Map.of("stock", s, "tech", t, "user", u);
    }

    private int indexStockData() {
        int count = 0;
        
        // Detailed Equipment
        List<Map<String, Object>> equipment = stockClient.getAllEquipment();
        for (Map<String, Object> eq : equipment) {
            String text = String.format("Equipment: %s | SN: %s | Brand: %s | Model: %s | Status: %s | Supplier: %s",
                    eq.getOrDefault("equipmentName", "N/A"),
                    eq.getOrDefault("serialNumber", "N/A"),
                    eq.getOrDefault("brand", "N/A"),
                    eq.getOrDefault("model", "N/A"),
                    eq.getOrDefault("status", "N/A"),
                    eq.getOrDefault("supplier", "N/A"));
            vectorStore.storeAll(List.of(text), "stock_manager", "STOCK_STATUS", null);
            count++;
        }

        // Detailed Suppliers (including Rating)
        List<Map<String, Object>> suppliers = stockClient.getAllSuppliers();
        for (Map<String, Object> sup : suppliers) {
            String text = String.format("Supplier: %s | Contact: %s | Rating: %s stars | Review: %s | Email: %s | Category: %s",
                    sup.getOrDefault("companyName", "N/A"),
                    sup.getOrDefault("contactPerson", "N/A"),
                    sup.getOrDefault("rating", "0"),
                    sup.getOrDefault("note", "No review"),
                    sup.getOrDefault("email", "N/A"),
                    sup.getOrDefault("category", "N/A"));
            vectorStore.storeAll(List.of(text), "stock_manager", "SUPPLIER_INFO", null);
            count++;
        }

        // Detailed Categories
        List<Map<String, Object>> categories = stockClient.getAllCategories();
        for (Map<String, Object> cat : categories) {
            String text = String.format("Category: %s | Icon: %s",
                    cat.getOrDefault("name", "N/A"),
                    cat.getOrDefault("icon", "N/A"));
            vectorStore.storeAll(List.of(text), "stock_manager", "CATEGORY_INFO", null);
            count++;
        }

        return count;
    }

    private int indexTechData() {
        List<Map<String, Object>> reqs = technicianClient.getAllPartRequests();
        for (Map<String, Object> r : reqs) {
            String text = String.format("Request: %s | Priority: %s | Status: %s", r.get("description"), r.get("priority"), r.get("status"));
            vectorStore.storeAll(List.of(text), "technician", "REQUEST_STATUS", null);
        }
        return reqs.size();
    }

    private int indexUserData() {
        List<Map<String, Object>> tasks = userClient.getAllTasks();
        for (Map<String, Object> t : tasks) {
            String text = String.format("Task: %s | Status: %s | Date: %s", t.get("title"), t.get("status"), t.get("date"));
            vectorStore.storeAll(List.of(text), "all", "SCHEDULE_INFO", null);
        }
        return tasks.size();
    }
}
