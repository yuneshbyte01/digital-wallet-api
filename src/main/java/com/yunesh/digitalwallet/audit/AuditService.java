package com.yunesh.digitalwallet.audit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditRepository auditRepository;

    @Async
    @Transactional
    public void logAction(AuditAction action,
                          UUID userId,
                          String ipAddress,
                          String userAgent,
                          String metadata) {
        try {
            Map<String, Object> metadataMap = null;
            if (metadata != null) {
                metadataMap = Map.of("info", metadata);
            }

            AuditLog auditLog = AuditLog.builder()
                    .userId(userId)
                    .action(action)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .metadata(metadataMap)
                    .build();

            auditRepository.save(auditLog);
        } catch (Exception e) {
            log.error("Failed to save audit log for action {}: {}",
                    action, e.getMessage());
        }
    }
}