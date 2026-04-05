package com.example.stockmanagermicroservice.seeder;

import com.example.stockmanagermicroservice.model.EquipmentCategory;
import com.example.stockmanagermicroservice.repository.EquipmentCategoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class DatabaseSeeder {

    @Bean
    CommandLineRunner initCategories(EquipmentCategoryRepository repository) {
        return args -> {
            if (repository.count() == 0) {
                System.out.println("Seeding default Equipment Categories into the database...");

                List<EquipmentCategory> defaults = Arrays.asList(
                        createCategory("DEVICE", "monitor", Arrays.asList("Laptop", "System Unit", "Desktop", "Smartphone", "Tablet")),
                        createCategory("PERIPHERAL", "mouse", Arrays.asList("Monitor", "Keyboard", "Mouse", "Printer", "Scanner", "Headset", "Webcam", "HDMI Cable", "USB Cable", "Charger", "Adapter", "Docking Station", "USB Hub")),
                        createCategory("NETWORK", "wifi", Arrays.asList("Router", "Switch", "Access Point", "Firewall")),
                        createCategory("STORAGE", "hard-drive", Arrays.asList("SSD", "HDD", "NVMe", "USB Flash", "Ext. HDD")),
                        createCategory("COMPONENT", "cpu", Arrays.asList("RAM", "CPU", "GPU", "Motherboard", "NIC"))
                );

                repository.saveAll(defaults);
                System.out.println("Seeding complete!");
            } else {
                System.out.println("Equipment Categories already exist. Searching for any missing defaults...");
                // Optionally handle missing defaults
            }
        };
    }

    private EquipmentCategory createCategory(String name, String icon, List<String> types) {
        EquipmentCategory category = new EquipmentCategory();
        category.setName(name);
        category.setIcon(icon);
        category.setTypes(types);
        return category;
    }
}
