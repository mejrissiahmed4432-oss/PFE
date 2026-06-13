package com.example.employeemicroservice.config;

import com.example.employeemicroservice.blockchain.BlockchainAuditService;
import com.example.employeemicroservice.blockchain.AuditEventRequest;
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

@Aspect
@Component
public class BlockchainAuditAspect {

    private static final Logger logger = LoggerFactory.getLogger(BlockchainAuditAspect.class);

    @Autowired
    private BlockchainAuditService blockchainAuditService;

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

            AuditEventRequest auditRequest = new AuditEventRequest(
                    userId,
                    userName,
                    userRole,
                    blockchainTraceable.action(),
                    "Method: " + methodName + " | Executed successfully.",
                    ipAddress
            );

            new Thread(() -> {
                try {
                    blockchainAuditService.logAction(auditRequest);
                    logger.info("[BLOCKCHAIN-AOP] Audit log processed successfully.");
                } catch (Exception e) {
                    logger.error("[BLOCKCHAIN-AOP] Failed to process audit log: {}", e.getMessage());
                }
            }).start();

        } catch (Exception ex) {
            logger.error("[BLOCKCHAIN-AOP] Critical error in Aspect: {}", ex.getMessage(), ex);
        }
    }
}
