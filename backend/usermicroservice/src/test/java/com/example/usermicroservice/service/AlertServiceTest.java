package com.example.usermicroservice.service;

import com.example.usermicroservice.model.Alert;
import com.example.usermicroservice.repository.AlertRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MongoTemplate mongoTemplate;

    private AlertService alertService;

    @BeforeEach
    void setUp() {
        alertService = new AlertService();
        ReflectionTestUtils.setField(alertService, "alertRepository", alertRepository);
        ReflectionTestUtils.setField(alertService, "messagingTemplate", messagingTemplate);
        ReflectionTestUtils.setField(alertService, "mongoTemplate", mongoTemplate);
    }

    @Test
    void createOrUpdateAlertUpdatesExistingActiveAlertByKey() {
        Alert existing = new Alert("WARRANTY_EXPIRED_eq1", "Old", "Old", "SYSTEM", "MEDIUM", "ROLE", "STOCK_MANAGER");
        when(alertRepository.findByKeyAndStatus("WARRANTY_EXPIRED_eq1", "ACTIVE")).thenReturn(Optional.of(existing));
        when(alertRepository.save(any(Alert.class))).thenAnswer(invocation -> invocation.getArgument(0));

        alertService.createOrUpdateAlert(
                "WARRANTY_EXPIRED_eq1",
                "WARRANTY_EXPIRED",
                "HIGH",
                "ROLE",
                "STOCK_MANAGER",
                "Warranty Expired",
                "Expired yesterday");

        ArgumentCaptor<Alert> captor = ArgumentCaptor.forClass(Alert.class);
        verify(alertRepository).save(captor.capture());
        Alert saved = captor.getValue();

        assertThat(saved.getType()).isEqualTo("WARRANTY_EXPIRED");
        assertThat(saved.getPriority()).isEqualTo("HIGH");
        assertThat(saved.getTargetType()).isEqualTo("ROLE");
        assertThat(saved.getTargetId()).isEqualTo("STOCK_MANAGER");
        assertThat(saved.getTitle()).isEqualTo("Warranty Expired");
        assertThat(saved.getMessage()).isEqualTo("Expired yesterday");
    }

    @Test
    void getAllAlertsOnlyIncludesMatchingUserOrRoleTargets() {
        when(mongoTemplate.find(any(Query.class), eq(Alert.class))).thenReturn(List.of());

        alertService.getAllAlerts("tech-1", "TECHNICIAN");

        ArgumentCaptor<Query> captor = ArgumentCaptor.forClass(Query.class);
        verify(mongoTemplate).find(captor.capture(), eq(Alert.class));

        String queryJson = captor.getValue().getQueryObject().toJson();
        assertThat(queryJson).contains("\"targetType\": \"USER\"");
        assertThat(queryJson).contains("\"targetId\": \"tech-1\"");
        assertThat(queryJson).contains("\"targetType\": \"ROLE\"");
        assertThat(queryJson).contains("\"targetId\": \"TECHNICIAN\"");
        assertThat(queryJson).doesNotContain("\"$exists\": false");
    }
}
