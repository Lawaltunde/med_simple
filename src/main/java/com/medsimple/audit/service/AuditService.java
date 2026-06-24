package com.medsimple.audit.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.medsimple.audit.entity.AuditLog;
import com.medsimple.audit.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuditService {

    // Self-contained mapper: audit details are always a small map of scalars, so the default
    // serializer is correct and we avoid coupling this service to the web ObjectMapper bean.
    private static final ObjectMapper MAPPER = new ObjectMapper();

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

    /**
     * Log with structured metadata. {@code details} is serialized to JSON before it reaches the
     * {@code jsonb} column — callers pass a map of fields, never a raw string, so it is impossible
     * to write a value the database rejects as invalid JSON.
     */
    public void log(UUID actorId, String action, String resourceType, UUID resourceId,
                    String ipAddress, Map<String, ?> details) {
        AuditLog entry = AuditLog.builder()
                .userId(actorId)
                .action(action)
                .resourceType(resourceType)
                .resourceId(resourceId)
                .ipAddress(ipAddress)
                .details(toJson(details))
                .build();
        auditLogRepository.save(entry);
    }

    private String toJson(Map<String, ?> details) {
        if (details == null || details.isEmpty()) {
            return null;
        }
        try {
            return MAPPER.writeValueAsString(details);
        } catch (JsonProcessingException e) {
            // Never let an audit-metadata serialization fault break the audited operation.
            throw new IllegalArgumentException("Audit details could not be serialized to JSON", e);
        }
    }
}
