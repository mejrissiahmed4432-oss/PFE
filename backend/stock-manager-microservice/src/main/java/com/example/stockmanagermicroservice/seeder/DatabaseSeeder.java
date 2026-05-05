package com.example.stockmanagermicroservice.seeder;

import com.example.stockmanagermicroservice.dto.ApplicationDTO;
import com.example.stockmanagermicroservice.model.CategoryType;
import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.repository.ApplicationRepository;
import com.example.stockmanagermicroservice.repository.EquipmentCategoryRepository;
import com.example.stockmanagermicroservice.service.ApplicationService;
import org.bson.Document;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class DatabaseSeeder {

    /**
     * Migration + Seed runner.
     *
     * Step 1 – Migration (runs on every startup, safe to re-run):
     *   Reads raw BSON documents from the equipment_categories collection.
     *   If any document's 'types' array contains plain strings instead of
     *   CategoryType objects, it converts them to { name: <str>, requiresQrCode: false }.
     *   This fixes MappingException 500s caused by the old List<String> schema.
     *
     * Step 2 – Seed:
     *   If the collection is empty after migration, populate it with sensible defaults.
     */
    @Bean
    CommandLineRunner initCategories(EquipmentCategoryRepository repository,
                                     MongoTemplate mongoTemplate) {
        return args -> {
            migrateStringTypes(mongoTemplate);

            if (repository.count() == 0) {
                System.out.println("[Seeder] Seeding default Equipment Categories...");
                List<EquipmentCategory> defaults = Arrays.asList(
                        createCategory("DEVICE",     "monitor",    Arrays.asList("Laptop", "System Unit", "Desktop", "Smartphone", "Tablet"),                                                                          true),
                        createCategory("PERIPHERAL", "mouse",      Arrays.asList("Monitor", "Keyboard", "Mouse", "Printer", "Scanner", "Headset", "Webcam", "HDMI Cable", "USB Cable", "Charger", "Adapter", "Docking Station", "USB Hub"), false),
                        createCategory("NETWORK",    "wifi",       Arrays.asList("Router", "Switch", "Access Point", "Firewall"),                                                                                      true),
                        createCategory("STORAGE",    "hard-drive", Arrays.asList("SSD", "HDD", "NVMe", "USB Flash", "Ext. HDD"),                                                                                       true),
                        createCategory("COMPONENT",  "cpu",        Arrays.asList("RAM", "CPU", "GPU", "Motherboard", "NIC"),                                                                                           false)
                );
                repository.saveAll(defaults);
                System.out.println("[Seeder] Seeding complete.");
            } else {
                System.out.println("[Seeder] Equipment Categories already exist. Skipping seed.");
            }
        };
    }

    @Bean
    CommandLineRunner initApplications(ApplicationRepository repository,
                                       ApplicationService applicationService) {
        return args -> {
            if (repository.count() == 0) {
                System.out.println("[Seeder] Seeding default Applications...");
                
                seedApp(applicationService, "Google Chrome", "v120.0", "Google", "Browser", "Free", null, 0);
                seedApp(applicationService, "Visual Studio Code", "v1.85", "Microsoft", "Utility", "Free", null, 0);
                seedApp(applicationService, "Microsoft Word", "v2024", "Microsoft", "Office", "Subscription", "XXXXX-XXXXX-XXXXX", 50);
                seedApp(applicationService, "Adobe Photoshop", "v2024", "Adobe", "Utility", "Paid", "YYYYY-YYYYY-YYYYY", 10);
                seedApp(applicationService, "Slack", "v4.36", "Slack Technologies", "Other", "Free", null, 0);
                seedApp(applicationService, "Docker Desktop", "v4.26", "Docker Inc.", "Utility", "Free", null, 0);

                System.out.println("[Seeder] Application seeding complete.");
            } else {
                System.out.println("[Seeder] Applications already exist. Skipping seed.");
            }
        };
    }

    private void seedApp(ApplicationService service, String name, String version, String vendor, 
                         String category, String licenseType, String licenseKey, int totalLicenses) {
        ApplicationDTO dto = new ApplicationDTO();
        dto.setName(name);
        dto.setVersion(version);
        dto.setVendor(vendor);
        dto.setCategory(category);
        dto.setLicenseType(licenseType);
        dto.setLicenseKey(licenseKey);
        dto.setTotalLicenses(totalLicenses);
        dto.setStatus("Active");
        service.createApplication(dto);
    }

    /**
     * Reads every raw document in equipment_categories. For each one, if the
     * 'types' array holds plain String entries, replaces them with
     * { name: <string>, requiresQrCode: false } sub-documents and saves the
     * document back.  Documents that already use the object format are untouched.
     */
    private void migrateStringTypes(MongoTemplate mongoTemplate) {
        List<Document> rawDocs = mongoTemplate.find(new Query(), Document.class, "equipment_categories");
        for (Document doc : rawDocs) {
            Object typesRaw = doc.get("types");
            if (!(typesRaw instanceof List)) continue;

            List<?> typesList = (List<?>) typesRaw;
            boolean needsMigration = typesList.stream().anyMatch(e -> e instanceof String);
            if (!needsMigration) continue;

            System.out.println("[Seeder] Migrating types for category: " + doc.getString("name"));

            List<Document> migratedTypes = new ArrayList<>();
            for (Object entry : typesList) {
                if (entry instanceof String) {
                    Document typeDoc = new Document();
                    typeDoc.put("name", entry);
                    typeDoc.put("requiresQrCode", false);
                    migratedTypes.add(typeDoc);
                } else if (entry instanceof Document) {
                    // Already an object – keep as-is (ensure requiresQrCode key exists)
                    Document typeDoc = (Document) entry;
                    if (!typeDoc.containsKey("requiresQrCode")) {
                        typeDoc.put("requiresQrCode", false);
                    }
                    migratedTypes.add(typeDoc);
                }
            }

            doc.put("types", migratedTypes);

            // Replace the entire document using its _id
            mongoTemplate.save(doc, "equipment_categories");
        }
    }

    private EquipmentCategory createCategory(String name, String icon,
                                              List<String> typeNames, boolean requiresQrCode) {
        EquipmentCategory category = new EquipmentCategory();
        category.setName(name);
        category.setIcon(icon);
        List<CategoryType> types = typeNames.stream()
                .map(t -> new CategoryType(t, requiresQrCode, "Asset"))
                .collect(Collectors.toList());
        category.setTypes(types);
        return category;
    }
}
