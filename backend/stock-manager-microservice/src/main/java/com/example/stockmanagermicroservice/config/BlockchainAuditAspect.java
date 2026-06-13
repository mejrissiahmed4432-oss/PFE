package com.example.stockmanagermicroservice.config;

import com.example.stockmanagermicroservice.client.AuditClient;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@Aspect
@Component
public class BlockchainAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainAuditAspect.class);

    @Autowired
    private AuditClient auditClient;

    @AfterReturning(pointcut = "@annotation(blockchainTraceable)")
    public void logAfter(JoinPoint joinPoint, BlockchainTraceable blockchainTraceable) {
        logger.info("[BLOCKCHAIN-AOP] Interception de la methode : {}", joinPoint.getSignature().getName());
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();

            String userId = "system";
            String userName = "Service Interne";
            String userRole = "SYSTEM";
            String ipAddress = "unknown";

            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();

                    // Lire les headers injectes par l'intercepteur Angular
                    String headerCin = request.getHeader("X-User-CIN");
                    String headerName = request.getHeader("X-User-Name");
                    String headerRole = request.getHeader("X-User-Role");
                    String headerEmail = request.getHeader("X-User-Email");

                    logger.info("[BLOCKCHAIN-AOP] Headers recus -> CIN='{}', Nom='{}', Role='{}', Email='{}'",
                            headerCin, headerName, headerRole, headerEmail);

                    // On utilise temporairement l'email comme ID pour que le microservice Employee
                    // puisse retrouver le CIN exact
                    if (headerEmail != null && !headerEmail.isEmpty()) {
                        userId = headerEmail;
                    } else if (headerCin != null && !headerCin.isEmpty()) {
                        userId = headerCin;
                    }

                    if (headerName != null && !headerName.isEmpty()) {
                        userName = headerName;
                        if (headerEmail != null && !headerEmail.isEmpty()) {
                            userName = headerName + " (" + headerEmail + ")";
                        }
                    } else if (headerEmail != null && !headerEmail.isEmpty()) {
                        userName = headerEmail;
                    }

                    if (headerRole != null && !headerRole.isEmpty()) {
                        userRole = headerRole;
                    }

                    // IP du client
                    String forwarded = request.getHeader("X-Forwarded-For");
                    ipAddress = (forwarded != null) ? forwarded.split(",")[0] : request.getRemoteAddr();
                } else {
                    logger.warn("[BLOCKCHAIN-AOP] RequestContextHolder vide - pas de contexte HTTP disponible.");
                }
            } catch (Exception e) {
                logger.error("[BLOCKCHAIN-AOP] Erreur lors de la lecture des headers HTTP : {}", e.getMessage());
            }

            final String finalUserId = userId;
            final String finalUserName = userName;
            final String finalUserRole = userRole;
            final String finalIp = ipAddress;

            Map<String, Object> auditRequest = new HashMap<>();
            auditRequest.put("userId", finalUserId);
            auditRequest.put("userName", finalUserName);
            auditRequest.put("userRole", finalUserRole);
            auditRequest.put("action", blockchainTraceable.action());
            auditRequest.put("details", "Methode: " + methodName + " | Executee avec succes.");
            auditRequest.put("ipAddress", finalIp);

            logger.info("[BLOCKCHAIN-AOP] Envoi de la trace : action='{}', user='{}', role='{}'",
                    blockchainTraceable.action(), finalUserName, finalUserRole);

            // Appel Asynchrone pour ne pas ralentir la reponse principale
            new Thread(() -> {
                try {
                    auditClient.logEvent(auditRequest);
                    logger.info("[BLOCKCHAIN-AOP] Trace enregistree avec succes dans MongoDB.");
                } catch (Exception e) {
                    logger.error("[BLOCKCHAIN-AOP] ECHEC envoi trace : {}", e.getMessage());
                }
            }).start();

        } catch (Exception ex) {
            logger.error("[BLOCKCHAIN-AOP] Erreur critique dans l'Aspect : {}", ex.getMessage(), ex);
        }
    }
}
