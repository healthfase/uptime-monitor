package dev.uptime.monitor.service;

import dev.uptime.monitor.dto.DashboardResponse;
import dev.uptime.monitor.dto.SiteStatisticsResponse;
import dev.uptime.monitor.entity.Site;
import dev.uptime.monitor.repository.SiteRepository;
import dev.uptime.monitor.repository.CheckResultRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DashboardService {

    private final SiteRepository siteRepository;
    private final SiteService siteService;
    private final CheckResultRepository checkResultRepository;

    public DashboardService(
            SiteRepository siteRepository,
            SiteService siteService,
            CheckResultRepository checkResultRepository
    ) {
        this.siteRepository = siteRepository;
        this.siteService = siteService;
        this.checkResultRepository = checkResultRepository;
    }

    public DashboardResponse getDashboard() {

        List<Site> sites = siteRepository.findAll();

        List<SiteStatisticsResponse> statistics = sites.stream()
                .map(site -> siteService.getSiteStatistics(site.getId()))
                .toList();

        int totalSites = statistics.size();

        int onlineSites = (int) statistics.stream()
                .filter(SiteStatisticsResponse::online)
                .count();

        int offlineSites = totalSites - onlineSites;

        long totalChecks = 0;
        long successfulChecks = 0;

        for (Site site : sites) {
            totalChecks += checkResultRepository.countBySite(site);

            successfulChecks +=
                    checkResultRepository.countBySiteAndOnlineTrue(site);
        }

        double overallUptime = totalChecks > 0
                ? successfulChecks * 100.0 / totalChecks
                : 0.0;

        return new DashboardResponse(
                totalSites,
                onlineSites,
                offlineSites,
                overallUptime,
                statistics
        );
    }
}