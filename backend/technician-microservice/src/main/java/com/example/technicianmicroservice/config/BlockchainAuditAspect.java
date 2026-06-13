package com.example.technicianmicroservice.config;

import com.example.technicianmicroservice.client.AuditClient;
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
        logger.info("[BLOCKCHAIN-AOP] Interception: {}", joinPoint.getSignature().getName());
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getMethod().getName();

            String userId = "system", userName = "Service Interne", userRole = "SYSTEM", ipAddress = "unknown";

            try {
                ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
                if (attrs != null) {
                    HttpServletRequest request = attrs.getRequest();
                    String headerEmail = request.getHeader("X-User-Email");
                    String headerName  = request.getHeader("X-User-Name");
                    String headerRole  = request.getHeader("X-User-Role");
                    String headerCin   = request.getHeader("X-User-CIN");

                    if (headerEmail != null && !headerEmail.isEmpty()) userId = headerEmail;
                    else if (headerCin != null && !headerCin.isEmpty()) userId = headerCin;

                    if (headerName != null && !headerName.isEmpty())
                        userName = headerName + (headerEmail != null ? " (" + headerEmail + ")" : "");
                    else if (headerEmail != null) userName = headerEmail;

                    if (headerRole != null && !headerRole.isEmpty()) userRole = headerRole;

                    String forwarded = request.getHeader("X-Forwarded-For");
                    ipAddress = (forwarded != null) ? forwarded.split(",")[0] : request.getRemoteAddr();
                }
            } catch (Exception e) {
                logger.error("[BLOCKCHAIN-AOP] Error reading HTTP headers: {}", e.getMessage());
            }

            final String fu = userId, fn = userName, fr = userRole, fi = ipAddress;
            Map<String, Object> auditRequest = new HashMap<>();
            auditRequest.put("userId",    fu);
            auditRequest.put("userName",  fn);
            auditRequest.put("userRole",  fr);
            auditRequest.put("action",    blockchainTraceable.action());
            auditRequest.put("details",   "Method: " + methodName + " | Executed successfully.");
            auditRequest.put("ipAddress", fi);

            new Thread(() -> {
                try {
                    auditClient.logEvent(auditRequest);
                    logger.info("[BLOCKCHAIN-AOP] Audit log sent successfully.");
                } catch (Exception e) {
                    logger.error("[BLOCKCHAIN-AOP] Failed to send audit log: {}", e.getMessage());
                }
            }).start();

        } catch (Exception ex) {
            logger.error("[BLOCKCHAIN-AOP] Critical error in Aspect: {}", ex.getMessage(), ex);
        }
    }
}
