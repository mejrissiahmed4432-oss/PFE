package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Alert;
import com.example.usermicroservice.repository.AlertRepository;
import com.example.usermicroservice.repository.TicketRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.data.mongodb.core.index.IndexOperations;
import org.springframework.data.mongodb.core.index.PartialIndexFilter;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class AlertMaintenanceService {

    private static final String ACTIVE = "ACTIVE";
    private static final String RESOLVED = "RESOLVED";
    private static final String ACTIVE_KEY_INDEX = "unique_active_alert_key";

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private MongoTemplate mongoTemplate;

    @EventListener(ApplicationReadyEvent.class)
    public void cleanAndIndexActiveAlerts() {
        cleanupActiveAlerts();
        ensureActiveKeyIndex();
    }

    public void cleanupActiveAlerts() {
        List<Alert> activeAlerts = alertRepository.findByStatusOrderByCreatedAtDesc(ACTIVE);

        resolveBlankKeyAlerts(activeAlerts);
        resolveDuplicateActiveKeys(activeAlerts);
        retargetTicketAlerts(activeAlerts);
        retargetItManagerAlerts(activeAlerts);
        retargetStockManagerAlerts(activeAlerts);
        resolveNonCriticalAlerts(activeAlerts);
    }

    private void resolveBlankKeyAlerts(List<Alert> activeAlerts) {
        activeAlerts.stream()
                .filter(alert -> ACTIVE.equals(alert.getStatus()))
                .filter(alert -> isBlank(alert.getKey()))
                .forEach(this::resolveWithoutBroadcast);
    }

    private void resolveDuplicateActiveKeys(List<Alert> activeAlerts) {
        Map<String, List<Alert>> byKey = activeAlerts.stream()
                .filter(alert -> !isBlank(alert.getKey()))
                .collect(Collectors.groupingBy(Alert::getKey));

        byKey.values().stream()
                .filter(group -> group.size() > 1)
                .forEach(group -> {
                    Alert newest = group.stream()
                            .max(Comparator.comparing(this::sortDate))
                            .orElse(null);

                    group.stream()
                            .filter(alert -> newest != null && !Objects.equals(alert.getId(), newest.getId()))
                            .forEach(this::resolveWithoutBroadcast);
                });
    }

    private void retargetStockManagerAlerts(List<Alert> activeAlerts) {
        activeAlerts.stream()
                .filter(alert -> ACTIVE.equals(alert.getStatus()))
                .filter(this::isStockManagerCriticalAlert)
                .forEach(alert -> {
                    if (!"ROLE".equals(alert.getTargetType()) || !"STOCK_MANAGER".equals(alert.getTargetId())) {
                        alert.setTargetType("ROLE");
                        alert.setTargetId("STOCK_MANAGER");
                        alertRepository.save(alert);
                    }
                });
    }

    private void retargetItManagerAlerts(List<Alert> activeAlerts) {
        activeAlerts.stream()
                .filter(alert -> ACTIVE.equals(alert.getStatus()))
                .filter(this::isItManagerCriticalAlert)
                .forEach(alert -> {
                    if (!"ROLE".equals(alert.getTargetType()) || !"IT_MANAGER".equals(alert.getTargetId())) {
                        alert.setTargetType("ROLE");
                        alert.setTargetId("IT_MANAGER");
                        alertRepository.save(alert);
                    }
                });
    }

    private void retargetTicketAlerts(List<Alert> activeAlerts) {
        activeAlerts.stream()
                .filter(this::isTicketAlert)
                .filter(alert -> ACTIVE.equals(alert.getStatus()))
                .forEach(alert -> {
                    String ticketId = extractTicketId(alert.getKey());
                    if (isBlank(ticketId)) {
                        return;
                    }

                    ticketRepository.findById(ticketId).ifPresent(ticket -> {
                        String targetType = isBlank(ticket.getAssignedTo()) ? "ROLE" : "USER";
                        String targetId = isBlank(ticket.getAssignedTo()) ? "TECHNICIAN" : ticket.getAssignedTo();

                        if (!targetType.equals(alert.getTargetType()) || !targetId.equals(alert.getTargetId())) {
                            alert.setTargetType(targetType);
                            alert.setTargetId(targetId);
                            alertRepository.save(alert);
                        }
                    });
                });
    }

    private void resolveNonCriticalAlerts(List<Alert> activeAlerts) {
        activeAlerts.stream()
                .filter(alert -> ACTIVE.equals(alert.getStatus()))
                .filter(this::isNonCritical)
                .forEach(this::resolveWithoutBroadcast);
    }

    private boolean isTicketAlert(Alert alert) {
        String key = normalize(alert.getKey());
        String type = normalize(alert.getType());
        return key.startsWith("TICKET_OVERDUE_")
                || key.startsWith("TICKET_CANCELLED_")
                || "TICKET_OVERDUE".equals(type)
                || "TICKET_CANCELLED".equals(type);
    }

    private boolean isNonCritical(Alert alert) {
        String key = normalize(alert.getKey());
        String type = normalize(alert.getType());

        if (key.startsWith("WARRANTY_EXPIRING_") || "WARRANTY_EXPIRING".equals(type)) {
            return true;
        }
        if (key.startsWith("LOW_STOCK_") || "LOW_STOCK".equals(type)) {
            return true;
        }
        if (key.startsWith("SHELF_LOW_") || key.startsWith("SHELF_FULL_")) {
            return true;
        }
        return (key.startsWith("REQUEST_PENDING_") || "REQUEST_PENDING".equals(type))
                && !"HIGH".equalsIgnoreCase(alert.getPriority());
    }

    private boolean isStockManagerCriticalAlert(Alert alert) {
        String key = normalize(alert.getKey());
        String type = normalize(alert.getType());

        return key.startsWith("WARRANTY_EXPIRED_")
                || key.startsWith("OUT_OF_STOCK_")
                || key.startsWith("SHELF_EMPTY_")
                || "WARRANTY_EXPIRED".equals(type)
                || "OUT_OF_STOCK".equals(type)
                || "SHELF_EMPTY".equals(type);
    }

    private boolean isItManagerCriticalAlert(Alert alert) {
        String key = normalize(alert.getKey());
        String type = normalize(alert.getType());

        return key.startsWith("OS_LICENSE_DEPLETED_")
                || key.startsWith("SOFTWARE_LICENSE_DEPLETED_")
                || key.startsWith("LICENSE_POOL_EXPIRED_")
                || "LICENSE_DEPLETED".equals(type)
                || "LICENSE_POOL_DEPLETED".equals(type)
                || "LICENSE_POOL_EXPIRED".equals(type);
    }

    private String extractTicketId(String key) {
        if (key == null) {
            return null;
        }
        if (key.startsWith("TICKET_OVERDUE_")) {
            return key.substring("TICKET_OVERDUE_".length());
        }
        if (key.startsWith("TICKET_CANCELLED_")) {
            return key.substring("TICKET_CANCELLED_".length());
        }
        return null;
    }

    private void resolveWithoutBroadcast(Alert alert) {
        if (!ACTIVE.equals(alert.getStatus())) {
            return;
        }

        alert.setStatus(RESOLVED);
        alert.setResolvedAt(LocalDateTime.now());
        alertRepository.save(alert);
    }

    private LocalDateTime sortDate(Alert alert) {
        if (alert.getCreatedAt() != null) {
            return alert.getCreatedAt();
        }
        if (alert.getLastSentAt() != null) {
            return alert.getLastSentAt();
        }
        return LocalDateTime.MIN;
    }

    private void ensureActiveKeyIndex() {
        IndexOperations indexOps = mongoTemplate.indexOps(Alert.class);
        dropLegacyKeyIndex(indexOps, "key");
        dropLegacyKeyIndex(indexOps, "key_1");

        Index index = new Index()
                .on("key", Sort.Direction.ASC)
                .unique()
                .named(ACTIVE_KEY_INDEX)
                .partial(PartialIndexFilter.of(new Criteria().andOperator(
                        Criteria.where("status").is(ACTIVE),
                        Criteria.where("key").exists(true))));

        indexOps.ensureIndex(index);
    }

    private void dropLegacyKeyIndex(IndexOperations indexOps, String name) {
        if (ACTIVE_KEY_INDEX.equals(name)) {
            return;
        }

        try {
            indexOps.dropIndex(name);
        } catch (RuntimeException ignored) {
            // Missing index or insufficient metadata; ensureIndex below is still safe to retry.
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
