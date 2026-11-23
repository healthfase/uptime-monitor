
package dev.uptime.monitor.dto;

import java.time.Instant;

public record CheckHistoryResponse(
        Long id,
        boolean online,
        int statusCode,
        long responseTimeMs,
        String message,
        Instant checkedAt
) {}
