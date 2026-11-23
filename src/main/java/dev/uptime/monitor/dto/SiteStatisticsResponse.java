
package dev.uptime.monitor.dto;

public record SiteStatisticsResponse(
        Long siteId,
        String url,
        String name,
        boolean online,
        int statusCode,
        long responseTimeMs,
        long totalChecks,
        long successfulChecks,
        long failedChecks,
        double uptimePercentage,
        double averageResponseTimeMs
) {}
