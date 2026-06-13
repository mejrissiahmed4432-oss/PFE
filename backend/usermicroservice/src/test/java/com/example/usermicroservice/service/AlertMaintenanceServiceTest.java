package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Alert;
import com.example.usermicroservice.model.Ticket;
import com.example.usermicroservice.repository.AlertRepository;
import com.example.usermicroservice.repository.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertMaintenanceServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private MongoTemplate mongoTemplate;

    private AlertMaintenanceService maintenanceService;

    @BeforeEach
    void setUp() {
        maintenanceService = new AlertMaintenanceService();
        ReflectionTestUtils.setField(maintenanceService, "alertRepository", alertRepository);
        ReflectionTestUtils.setField(maintenanceService, "ticketRepository", ticketRepository);
        ReflectionTestUtils.setField(maintenanceService, "mongoTemplate", mongoTemplate);
    }

    @Test
    void cleanupActiveAlertsDeduplicatesRetargetsTicketsAndResolvesNonCriticalAlerts() {
        Alert olderDuplicate = alert("old", "WARRANTY_EXPIRED_eq1", "WARRANTY_EXPIRED", "HIGH");
        olderDuplicate.setCreatedAt(LocalDateTime.now().minusHours(2));
        Alert newestDuplicate = alert("new", "WARRANTY_EXPIRED_eq1", "WARRANTY_EXPIRED", "HIGH");
        newestDuplicate.setCreatedAt(LocalDateTime.now());

        Alert ticketAlert = alert("ticket", "TICKET_OVERDUE_t1", "TICKET_OVERDUE", "HIGH");
        ticketAlert.setTargetType("ROLE");
        ticketAlert.setTargetId("STOCK_MANAGER");

        Alert lowStockAlert = alert("low", "LOW_STOCK_eq2", "LOW_STOCK", "MEDIUM");
        Alert licenseAlert = alert("license", "OS_LICENSE_DEPLETED_os1", "LICENSE_DEPLETED", "HIGH");
        licenseAlert.setTargetType("OS");
        licenseAlert.setTargetId("os1");

        Ticket ticket = new Ticket();
        ticket.setId("t1");
        ticket.setAssignedTo("tech-1");

        when(alertRepository.findByStatusOrderByCreatedAtDesc("ACTIVE"))
                .thenReturn(List.of(olderDuplicate, newestDuplicate, ticketAlert, lowStockAlert, licenseAlert));
        when(ticketRepository.findById("t1")).thenReturn(Optional.of(ticket));

        maintenanceService.cleanupActiveAlerts();

        assertThat(olderDuplicate.getStatus()).isEqualTo("RESOLVED");
        assertThat(newestDuplicate.getStatus()).isEqualTo("ACTIVE");
        assertThat(ticketAlert.getTargetType()).isEqualTo("USER");
        assertThat(ticketAlert.getTargetId()).isEqualTo("tech-1");
        assertThat(lowStockAlert.getStatus()).isEqualTo("RESOLVED");
        assertThat(licenseAlert.getTargetType()).isEqualTo("ROLE");
        assertThat(licenseAlert.getTargetId()).isEqualTo("IT_MANAGER");

        verify(alertRepository, atLeastOnce()).save(olderDuplicate);
        verify(alertRepository, atLeastOnce()).save(ticketAlert);
        verify(alertRepository, atLeastOnce()).save(lowStockAlert);
        verify(alertRepository, atLeastOnce()).save(licenseAlert);
    }

    private Alert alert(String id, String key, String type, String priority) {
        Alert alert = new Alert(key, type, type, type, priority, "ROLE", "STOCK_MANAGER");
        alert.setId(id);
        return alert;
    }
}
