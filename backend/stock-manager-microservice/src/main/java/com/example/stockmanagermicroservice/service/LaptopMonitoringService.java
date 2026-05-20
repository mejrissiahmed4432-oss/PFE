package com.example.stockmanagermicroservice.service;

import com.example.stockmanagermicroservice.dto.DeptPcSummaryDTO;
import com.example.stockmanagermicroservice.dto.LaptopStatusDTO;
import com.example.stockmanagermicroservice.model.Department;
import com.example.stockmanagermicroservice.model.Equipment;
import com.example.stockmanagermicroservice.repository.DepartmentRepository;
import com.example.stockmanagermicroservice.repository.EquipmentRepository;
import com.example.stockmanagermicroservice.repository.MachineRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Builds the full department/laptop monitoring snapshot by cross-referencing:
 * equipment (MongoDB) → machines (IP mapping) → Prometheus
 * (UP/CPU/RAM/Disk/Network)
 */
@Service
public class LaptopMonitoringService {

    private static final String PROMETHEUS_URL = "http://0.0.0.0:9090/api/v1/query";
    private static final String QUERY_UP = "up{job=\"windows-pcs\"}";
    private static final String QUERY_CPU = "100 - (avg by (instance) (irate(windows_cpu_time_total{mode=\"idle\"}[10s])) * 100)";
    private static final String QUERY_RAM = "100 * (1 - (windows_memory_available_bytes / windows_cs_physical_memory_bytes))";
    private static final String QUERY_DISK = "100 - (100 * windows_logical_disk_free_bytes{volume=~\"[A-Z]:\"} / windows_logical_disk_size_bytes{volume=~\"[A-Z]:\"})";
    private static final String QUERY_NET_IN = "sum by (instance) (irate(windows_net_bytes_received_total[1m]))";
    private static final String QUERY_NET_OUT = "sum by (instance) (irate(windows_net_bytes_sent_total[1m]))";
    private static final String QUERY_TOP_PROC = "topk by (instance) (3, windows_process_working_set_bytes{process!=\"Idle\", process!=\"_Total\"})";
    private static final String QUERY_OS = "windows_os_info";
    private static final String QUERY_UPTIME = "time() - windows_system_system_up_time";
    private static final String QUERY_RAM_TOTAL = "windows_cs_physical_memory_bytes";
    private static final String QUERY_RAM_AVAILABLE = "windows_memory_available_bytes";
    private static final String QUERY_DISK_TOTAL = "sum by (instance) (windows_logical_disk_size_bytes{volume=~\"[A-Z]:\"})";
    private static final String QUERY_DISK_FREE = "sum by (instance) (windows_logical_disk_free_bytes{volume=~\"[A-Z]:\"})";
    private static final String QUERY_TEMP = "max by (instance) (windows_thermalzone_temperature_celsius)";

    @Autowired
    private EquipmentRepository equipmentRepository;
    @Autowired
    private MachineRepository machineRepository;
    @Autowired
    private DepartmentRepository departmentRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<String, Long> lastSeenCache = new java.util.concurrent.ConcurrentHashMap<>();

    public List<DeptPcSummaryDTO> getDeptPcStatus() {
        Map<String, List<LaptopStatusDTO>> byDept = new LinkedHashMap<>();
        departmentRepository.findAll().forEach(dept -> {
            if (dept.getName() != null && !dept.getName().equalsIgnoreCase("stock")) {
                byDept.put(dept.getName(), new ArrayList<>());
            }
        });

        List<Equipment> laptops = equipmentRepository.findAllExcludingFiles().stream()
                .filter(e -> e.getType() != null && e.getType().equalsIgnoreCase("laptop"))
                .filter(e -> e.getDepartment() != null && !e.getDepartment().equalsIgnoreCase("stock"))
                .collect(Collectors.toList());

        if (laptops.isEmpty()) {
            return byDept.entrySet().stream()
                    .map(e -> new DeptPcSummaryDTO(e.getKey(), e.getValue()))
                    .sorted(Comparator.comparing(DeptPcSummaryDTO::getDepartmentName))
                    .collect(Collectors.toList());
        }

        List<String> serials = laptops.stream()
                .map(Equipment::getSerialNumber).filter(Objects::nonNull).collect(Collectors.toList());
        Map<String, String> serialToIp = new HashMap<>();
        Map<String, String> serialToMac = new HashMap<>();
        machineRepository.findBySerialIn(serials).forEach(m -> {
            if (m.getSerial() != null) {
                if (m.getIp() != null) {
                    serialToIp.put(m.getSerial().trim(), m.getIp().trim());
                }
                if (m.getMac() != null) {
                    serialToMac.put(m.getSerial().trim(), m.getMac().trim());
                }
            }
        });

        // Parallelize all Prometheus queries to stabilize update intervals (2s)
        java.util.concurrent.CompletableFuture<Map<String, String>> statusFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchInstanceMap(QUERY_UP));
        java.util.concurrent.CompletableFuture<Map<String, Double>> cpuFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_CPU));
        java.util.concurrent.CompletableFuture<Map<String, Double>> ramFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_RAM));
        java.util.concurrent.CompletableFuture<Map<String, Map<String, Double>>> disksFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDiskMap(QUERY_DISK));
        java.util.concurrent.CompletableFuture<Map<String, Double>> netInFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_NET_IN));
        java.util.concurrent.CompletableFuture<Map<String, Double>> netOutFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_NET_OUT));
        java.util.concurrent.CompletableFuture<Map<String, List<com.example.stockmanagermicroservice.dto.ProcessInfoDTO>>> topProcsFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchTopProcesses(QUERY_TOP_PROC));
        java.util.concurrent.CompletableFuture<Map<String, String>> osFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchOSMap(QUERY_OS));
        java.util.concurrent.CompletableFuture<Map<String, Double>> uptimeFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_UPTIME));
        java.util.concurrent.CompletableFuture<Map<String, Double>> ramTotalFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_RAM_TOTAL));
        java.util.concurrent.CompletableFuture<Map<String, Double>> ramFreeFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_RAM_AVAILABLE));
        java.util.concurrent.CompletableFuture<Map<String, Double>> diskTotalFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_DISK_TOTAL));
        java.util.concurrent.CompletableFuture<Map<String, Double>> diskFreeFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_DISK_FREE));
        java.util.concurrent.CompletableFuture<Map<String, Double>> tempFuture = java.util.concurrent.CompletableFuture
                .supplyAsync(() -> fetchDoubleMap(QUERY_TEMP));

        // Wait for all queries to complete
        java.util.concurrent.CompletableFuture.allOf(
                statusFuture, cpuFuture, ramFuture, disksFuture, netInFuture, netOutFuture,
                topProcsFuture, osFuture, uptimeFuture, ramTotalFuture, ramFreeFuture,
                diskTotalFuture, diskFreeFuture, tempFuture).join();

        // Retrieve results from futures
        Map<String, String> instanceStatus = statusFuture.join();
        Map<String, Double> instanceCpu = cpuFuture.join();
        Map<String, Double> instanceRam = ramFuture.join();
        Map<String, Map<String, Double>> instanceDisks = disksFuture.join();
        Map<String, Double> instanceNetIn = netInFuture.join();
        Map<String, Double> instanceNetOut = netOutFuture.join();
        Map<String, List<com.example.stockmanagermicroservice.dto.ProcessInfoDTO>> instanceTopProcs = topProcsFuture
                .join();
        Map<String, String> instanceOS = osFuture.join();
        Map<String, Double> instanceUptime = uptimeFuture.join();
        Map<String, Double> instanceRamTotal = ramTotalFuture.join();
        Map<String, Double> instanceRamFree = ramFreeFuture.join();
        Map<String, Double> instanceDiskTotal = diskTotalFuture.join();
        Map<String, Double> instanceDiskFree = diskFreeFuture.join();
        Map<String, Double> instanceTemp = tempFuture.join();

        for (Equipment eq : laptops) {
            String serial = eq.getSerialNumber();
            String ip = null;
            String status;
            double cpu = 0, ram = 0, diskPercent = 0, temperature = 0;
            double totalRamGB = 0, freeRamGB = 0, totalDiskGB = 0, freeDiskGB = 0;
            Map<String, Double> disks = new HashMap<>();
            double netIn = 0, netOut = 0;
            String net = "Not Found", os = "Not Found", mac = "Not Found";
            if (serial != null) {
                mac = serialToMac.getOrDefault(serial.trim(), "Not Found");
            }
            String lastSeenStr = "Not Found";
            String uptimeStr = "Not Found";
            List<com.example.stockmanagermicroservice.dto.ProcessInfoDTO> procs = new ArrayList<>();

            if (serial == null || serial.isBlank() || !serialToIp.containsKey(serial.trim())) {
                status = "NOT_FOUND_YET";
            } else {
                ip = serialToIp.get(serial.trim());
                final String ipFinal = ip;

                Optional<String> inst = instanceStatus.keySet().stream()
                        .filter(i -> i.startsWith(ipFinal)).findFirst();

                if (inst.isPresent()) {
                    String fullInstance = inst.get();
                    status = instanceStatus.get(fullInstance);

                    if ("UP".equals(status)) {
                        lastSeenCache.put(fullInstance, System.currentTimeMillis());
                        lastSeenStr = "";
                    } else {
                        Long lastTime = lastSeenCache.get(fullInstance);
                        if (lastTime != null) {
                            lastSeenStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                                    .format(new java.util.Date(lastTime));
                        }
                    }

                    cpu = instanceCpu.getOrDefault(fullInstance, 0.0);
                    ram = instanceRam.getOrDefault(fullInstance, 0.0);
                    os = instanceOS.getOrDefault(fullInstance, "Not Found");
                    temperature = instanceTemp.getOrDefault(fullInstance, 0.0);

                    Double upSecs = instanceUptime.get(fullInstance);
                    if (upSecs != null) {
                        long secs = upSecs.longValue();
                        long h = secs / 3600;
                        long m = (secs % 3600) / 60;
                        uptimeStr = String.format("%dh %dm", h, m);
                    }

                    double totalRamBytes = instanceRamTotal.getOrDefault(fullInstance, 0.0);
                    double freeRamBytes = instanceRamFree.getOrDefault(fullInstance, 0.0);
                    double totalDiskBytes = instanceDiskTotal.getOrDefault(fullInstance, 0.0);
                    double freeDiskBytes = instanceDiskFree.getOrDefault(fullInstance, 0.0);

                    totalRamGB = totalRamBytes / (1024 * 1024 * 1024);
                    freeRamGB = freeRamBytes / (1024 * 1024 * 1024);
                    totalDiskGB = totalDiskBytes / (1024 * 1024 * 1024);
                    freeDiskGB = freeDiskBytes / (1024 * 1024 * 1024);

                    disks = instanceDisks.getOrDefault(fullInstance, new HashMap<>());
                    diskPercent = disks.getOrDefault("C:", 0.0);

                    netIn = instanceNetIn.getOrDefault(fullInstance, 0.0);
                    netOut = instanceNetOut.getOrDefault(fullInstance, 0.0);
                    double totalNetBytes = netIn + netOut;

                    if (totalNetBytes > 0) {
                        if (totalNetBytes > 1024 * 1024)
                            net = String.format("%.1f MB/s", totalNetBytes / (1024 * 1024));
                        else if (totalNetBytes > 1024)
                            net = String.format("%.1f KB/s", totalNetBytes / 1024);
                        else
                            net = String.format("%.0f B/s", totalNetBytes);
                    }

                    procs = instanceTopProcs.getOrDefault(fullInstance, new ArrayList<>());

                } else {
                    status = "DOWN";
                    Long lastTime = lastSeenCache.get(ipFinal + ":9182");
                    if (lastTime != null) {
                        lastSeenStr = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                                .format(new java.util.Date(lastTime));
                    }
                }
            }

            LaptopStatusDTO dto = new LaptopStatusDTO(
                    eq.getId(), eq.getEquipmentName(), serial,
                    eq.getBrand(), eq.getModel(),
                    eq.getDepartment(), ip, status);
            dto.setCpuPercent(cpu);
            dto.setRamPercent(ram);
            dto.setDiskPercent(diskPercent);
            dto.setTotalRam(totalRamGB);
            dto.setFreeRam(freeRamGB);
            dto.setTotalDisk(totalDiskGB);
            dto.setFreeDisk(freeDiskGB);
            dto.setTemperature(temperature);
            dto.setDiskVolumes(disks);
            dto.setNetworkSpeed(net);
            dto.setNetInSpeed(netIn);
            dto.setNetOutSpeed(netOut);
            dto.setTopProcesses(procs);
            dto.setOs(os);
            dto.setMacAddress(mac);
            dto.setLastSeen(lastSeenStr);
            dto.setUptime(uptimeStr);

            String dept = eq.getDepartment() != null ? eq.getDepartment() : "Unknown";
            byDept.putIfAbsent(dept, new ArrayList<>());
            byDept.get(dept).add(dto);
        }

        return byDept.entrySet().stream()
                .map(e -> new DeptPcSummaryDTO(e.getKey(), e.getValue()))
                .sorted(Comparator.comparing(DeptPcSummaryDTO::getDepartmentName))
                .collect(Collectors.toList());
    }

    private Map<String, String> fetchInstanceMap(String query) {
        Map<String, String> map = new HashMap<>();
        JsonNode results = queryPrometheus(query);
        if (results == null)
            return map;
        for (JsonNode res : results) {
            String instance = res.path("metric").path("instance").asText(null);
            if (instance == null)
                continue;
            JsonNode val = res.path("value");
            if (val.isArray() && val.size() >= 2) {
                map.put(instance, "1".equals(val.get(1).asText("0")) ? "UP" : "DOWN");
            }
        }
        return map;
    }

    private Map<String, Map<String, Double>> fetchDiskMap(String query) {
        Map<String, Map<String, Double>> map = new HashMap<>();
        JsonNode results = queryPrometheus(query);
        if (results == null)
            return map;
        for (JsonNode res : results) {
            String instance = res.path("metric").path("instance").asText(null);
            String volume = res.path("metric").path("volume").asText(null);
            if (instance == null || volume == null)
                continue;
            JsonNode val = res.path("value");
            if (val.isArray() && val.size() >= 2) {
                try {
                    double v = Double.parseDouble(val.get(1).asText("0"));
                    map.computeIfAbsent(instance, k -> new HashMap<>()).put(volume, Math.round(v * 10.0) / 10.0);
                } catch (Exception ignored) {
                }
            }
        }
        return map;
    }

    private Map<String, List<com.example.stockmanagermicroservice.dto.ProcessInfoDTO>> fetchTopProcesses(String query) {
        Map<String, List<com.example.stockmanagermicroservice.dto.ProcessInfoDTO>> map = new HashMap<>();
        JsonNode results = queryPrometheus(query);
        if (results == null)
            return map;
        for (JsonNode res : results) {
            String instance = res.path("metric").path("instance").asText(null);
            String processName = res.path("metric").path("process").asText("Unknown");
            if (instance == null)
                continue;
            JsonNode val = res.path("value");
            if (val.isArray() && val.size() >= 2) {
                try {
                    double bytes = Double.parseDouble(val.get(1).asText("0"));
                    double mb = Math.round((bytes / (1024 * 1024)) * 10.0) / 10.0;
                    map.computeIfAbsent(instance, k -> new ArrayList<>())
                            .add(new com.example.stockmanagermicroservice.dto.ProcessInfoDTO(processName, mb));
                } catch (Exception ignored) {
                }
            }
        }
        map.values().forEach(list -> list.sort(Comparator
                .comparingDouble(com.example.stockmanagermicroservice.dto.ProcessInfoDTO::getRamUsageMb).reversed()));
        return map;
    }

    private Map<String, Double> fetchDoubleMap(String query) {
        Map<String, Double> map = new HashMap<>();
        JsonNode results = queryPrometheus(query);
        if (results == null)
            return map;
        for (JsonNode res : results) {
            String instance = res.path("metric").path("instance").asText(null);
            if (instance == null)
                continue;
            JsonNode val = res.path("value");
            if (val.isArray() && val.size() >= 2) {
                try {
                    double v = Double.parseDouble(val.get(1).asText("0"));
                    map.put(instance, Math.round(v * 10.0) / 10.0);
                } catch (Exception ignored) {
                }
            }
        }
        return map;
    }

    private Map<String, String> fetchOSMap(String query) {
        Map<String, String> map = new HashMap<>();
        JsonNode results = queryPrometheus(query);
        if (results == null)
            return map;
        for (JsonNode res : results) {
            String instance = res.path("metric").path("instance").asText(null);
            String product = res.path("metric").path("product").asText("Not Found");
            if (instance != null)
                map.put(instance, product);
        }
        return map;
    }

    private JsonNode queryPrometheus(String query) {
        try {
            URI uri = UriComponentsBuilder.fromUriString(PROMETHEUS_URL)
                    .queryParam("query", query).build().toUri();
            String response = restTemplate.getForObject(uri, String.class);
            if (response == null)
                return null;
            return mapper.readTree(response).path("data").path("result");
        } catch (Exception e) {
            return null;
        }
    }
}
