package dev.uptime.monitor.dto;

import dev.uptime.monitor.entity.Site;

import java.util.List;

public record SitePageResponse(
        List<Site> sites,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}