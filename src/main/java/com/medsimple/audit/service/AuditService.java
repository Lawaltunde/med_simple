package com.medsimple.audit.service;

import com.medsimple.audit.entity.AuditLog;
import com.medsimple.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    private final AuditLogRepository auditLogRepository;

    public void log(UUID actorId, String action, String resourceType, UUID resourceId, String ipAddress) {
        AuditLog entry = AuditLog.builder()
                .userId(actorId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .build();
        auditLogRepository.save(entry);
    }

    public void log(UUID actorId, String action, String resourceType, UUID resourceId,
                    String ipAddress, String details) {
        AuditLog entry = AuditLog.builder()
                .userId(actorId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .details(details)
                .build();
        auditLogRepository.save(entry);
    }
}
