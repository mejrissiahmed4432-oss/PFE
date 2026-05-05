package com.example.technicianmicroservice.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/capabilities")
@CrossOrigin(origins = "*")
public class CapabilityController {

    @GetMapping("/{equipmentType}")
    public ResponseEntity<Map<String, Object>> getCapabilities(@PathVariable String equipmentType) {

        Map<String, List<String>> categoryTypes = new LinkedHashMap<>();
        String type = equipmentType.toLowerCase().trim();

        switch (type) {
            case "laptop":
            case "pc":
            case "desktop":
                categoryTypes.put("hardware", Arrays.asList("install", "replace", "upgrade", "remove", "repair"));
                categoryTypes.put("software", Arrays.asList("install", "update", "uninstall", "configure", "scan"));
                categoryTypes.put("maintenance", Arrays.asList("clean", "inspect", "test", "calibrate", "backup"));
                break;
            case "router":
                categoryTypes.put("configuration", Arrays.asList("configure", "reset", "backup", "restore"));
                categoryTypes.put("network", Arrays.asList("setup", "test", "troubleshoot", "optimize"));
                categoryTypes.put("firmware", Arrays.asList("update", "rollback", "verify"));
                break;
            case "server":
            case "rack server":
                categoryTypes.put("hardware", Arrays.asList("install", "replace", "upgrade", "remove", "repair"));
                categoryTypes.put("software", Arrays.asList("install", "update", "uninstall", "configure", "patch"));
                categoryTypes.put("services", Arrays.asList("start", "stop", "restart", "configure", "monitor", "migrate"));
                break;
            case "switch":
                categoryTypes.put("configuration", Arrays.asList("configure", "reset", "backup", "restore", "vlan"));
                categoryTypes.put("network", Arrays.asList("setup", "test", "troubleshoot", "optimize", "monitor"));
                categoryTypes.put("maintenance", Arrays.asList("clean", "inspect", "test", "replace-port"));
                break;
            case "printer":
            case "laser printer":
            case "inkjet printer":
                categoryTypes.put("hardware", Arrays.asList("replace", "install", "repair", "calibrate"));
                categoryTypes.put("maintenance", Arrays.asList("clean", "inspect", "test", "align", "refill"));
                break;
            default:
                categoryTypes.put("hardware", Arrays.asList("install", "replace", "repair", "inspect"));
                categoryTypes.put("maintenance", Arrays.asList("inspect", "test", "clean", "repair"));
                break;
        }

        Map<String, List<String>> categoryTargets = new LinkedHashMap<>();
        for (String cat : categoryTypes.keySet()) {
            categoryTargets.put(cat, getTargetsForCategory(cat));
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("equipmentType", equipmentType);
        response.put("categories", new ArrayList<>(categoryTypes.keySet()));
        response.put("categoryTypes", categoryTypes);
        response.put("categoryTargets", categoryTargets);

        return ResponseEntity.ok(response);
    }

    private List<String> getTargetsForCategory(String category) {
        switch (category) {
            case "hardware":
                return Arrays.asList("RAM", "CPU", "SSD", "HDD", "Battery", "Screen", "Keyboard",
                        "Fan", "Power Supply", "GPU", "Motherboard", "Network Card", "Other");
            case "software":
                return Arrays.asList("Operating System", "Drivers", "Antivirus", "Office Suite",
                        "Application", "Browser", "VPN Client", "Firmware", "Other");
            case "configuration":
                return Arrays.asList("IP Address", "Subnet Mask", "Gateway", "DNS", "VLAN",
                        "Firewall Rules", "NAT", "QoS", "DHCP", "Access Control", "Other");
            case "network":
                return Arrays.asList("LAN", "WAN", "WiFi", "VPN", "Routing", "Switching",
                        "Bandwidth", "Latency", "DNS", "DHCP", "Other");
            case "firmware":
                return Arrays.asList("Router Firmware", "Switch Firmware", "BIOS",
                        "Controller Firmware", "Other");
            case "maintenance":
                return Arrays.asList("Physical Cleaning", "Thermal Paste", "Cable Management",
                        "Cooling System", "Battery", "Ports", "Connectors", "Other");
            case "services":
                return Arrays.asList("Web Server", "Database", "Mail Server", "File Server",
                        "Authentication Service", "Backup Service", "Monitoring", "Load Balancer", "Other");
            default:
                return Arrays.asList("Other");
        }
    }
}
