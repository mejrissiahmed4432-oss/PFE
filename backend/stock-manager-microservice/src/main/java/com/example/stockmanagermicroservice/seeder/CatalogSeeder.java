package com.example.stockmanagermicroservice.seeder;

import com.example.stockmanagermicroservice.procurement.model.CatalogItem;
import com.example.stockmanagermicroservice.procurement.repository.CatalogItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class CatalogSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeeder.class);
    private final CatalogItemRepository repository;

    public CatalogSeeder(CatalogItemRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() == 0) {
            log.info("Seeding initial equipment catalog...");

            List<CatalogItem> items = Arrays.asList(
                    new CatalogItem("ThinkPad T14", "Laptops", "Lenovo ThinkPad T14s, 16GB RAM, 512GB SSD", "14-inch, Intel Core i5/i7 or AMD Ryzen", Arrays.asList("Mouse", "Laptop Bag", "Monitor"), Arrays.asList("TECH-SUP-01")),
                    new CatalogItem("MacBook Pro 14", "Laptops", "Apple MacBook Pro 14-inch M3", "14-inch, M3 Pro, 18GB RAM, 512GB SSD", Arrays.asList("USB-C Hub", "Magic Mouse", "Laptop Bag"), Arrays.asList("APPLE-B2B-01")),
                    new CatalogItem("Dell UltraSharp 27", "Monitors", "Dell UltraSharp 27 4K USB-C Hub Monitor - U2723QE", "27-inch 4K UHD, USB-C 90W", Arrays.asList("HDMI Cable", "DisplayPort Cable", "Monitor Arm"), Arrays.asList("TECH-SUP-01")),
                    new CatalogItem("Logitech MX Master 3S", "Peripherals", "Advanced Wireless Mouse", "Bluetooth, USB-C fast charging, Silent clicks", Arrays.asList("Keyboard", "Mousepad"), Arrays.asList("TECH-SUP-01")),
                    new CatalogItem("Logitech MX Keys Mini", "Peripherals", "Minimalist Wireless Illuminated Keyboard", "Bluetooth, USB-C charging", Arrays.asList("Mouse"), Arrays.asList("TECH-SUP-01")),
                    new CatalogItem("Ergonomic Office Chair", "Furniture", "Adjustable ergonomic office chair with lumbar support", "Mesh back, adjustable armrests", Arrays.asList("Standing Desk", "Footrest"), Arrays.asList("OFFICE-SUP-01")),
                    new CatalogItem("Standing Desk", "Furniture", "Electric height adjustable standing desk", "Dual motor, 120x60cm", Arrays.asList("Ergonomic Office Chair", "Anti-fatigue Mat"), Arrays.asList("OFFICE-SUP-01")),
                    new CatalogItem("Cisco Meraki MR46", "Network", "Wi-Fi 6 Indoor Access Point", "802.11ax, 4x4:4 MU-MIMO", Arrays.asList("Cat6 Cable", "PoE Switch"), Arrays.asList("NET-SUP-01")),
                    new CatalogItem("Jabra Evolve2 65", "Audio", "Wireless Professional Headset", "Noise-isolating, USB-A/USB-C Bluetooth adapter", Arrays.asList("Webcam"), Arrays.asList("TECH-SUP-01")),
                    new CatalogItem("Logitech Brio 4K", "Audio/Video", "Ultra HD Webcam", "4K Ultra HD video calling, HDR, Windows Hello", Arrays.asList("Headset"), Arrays.asList("TECH-SUP-01"))
            );

            repository.saveAll(items);
            log.info("Seeded {} catalog items successfully.", items.size());
        } else {
            log.info("Catalog already seeded.");
        }
    }
}
