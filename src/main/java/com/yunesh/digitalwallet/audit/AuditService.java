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
                          Map<String, Object> metadata) {
        try {
            AuditLog entry = AuditLog.builder()
                    .action(action)
                    .userId(userId)
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .metadata(metadata)
                    .build();
            auditRepository.save(entry);
        } catch (Exception ex) {
            log.error("Failed to write audit log [action={}]: {}", action, ex.getMessage());
        }
    }
}
