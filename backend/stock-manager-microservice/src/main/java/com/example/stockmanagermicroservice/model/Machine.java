package com.example.stockmanagermicroservice.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Represents a registered machine (PC) in the monitoring system.
 * The 'machines' collection is populated by the PowerShell agent running on each PC.
 * Fields:
 *  - serial: the PC's serial number (matches Equipment.serialNumber)
 *  - ip: the PC's IP address WITHOUT port (e.g., "192.168.1.5")
 */
@Document(collection = "machines")
public class Machine {

    @Id
    private String id;

    /** Serial number as reported by the Windows agent (matches equipment.serialNumber) */
    private String serial;

    /** IP address without port — e.g., "192.168.1.5" */
    private String ip;

    /** Optional hostname for display purposes */
    private String hostname;

    private String mac;

    public Machine() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getSerial() { return serial; }
    public void setSerial(String serial) { this.serial = serial; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getMac() { return mac; }
    public void setMac(String mac) { this.mac = mac; }
}
