package dev.uptime.monitor.dto;

import java.util.List;

public record DashboardResponse(
        int totalSites,
        int onlineSites,
        int offlineSites,
        double overallUptime,
        List<SiteStatisticsResponse> sites
) {}