
package dev.uptime.monitor.dto;

public record SiteCheckResponse(
        String url,
        boolean online,
        int statusCode,
        long responseTimeMs,
        String message
) {
}
