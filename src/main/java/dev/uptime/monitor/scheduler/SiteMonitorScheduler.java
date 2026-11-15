
package dev.uptime.monitor.scheduler;

import dev.uptime.monitor.config.MonitoredSites;
import dev.uptime.monitor.dto.SiteCheckResponse;
import dev.uptime.monitor.entity.Site;
import dev.uptime.monitor.entity.CheckResult;
import dev.uptime.monitor.repository.SiteRepository;
import dev.uptime.monitor.repository.CheckResultRepository;
import dev.uptime.monitor.service.MonitorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class SiteMonitorScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(SiteMonitorScheduler.class);

    private final MonitorService monitorService;
    private final SiteRepository siteRepository;
    private final CheckResultRepository checkResultRepository;

    public SiteMonitorScheduler(
            MonitorService monitorService,
            SiteRepository siteRepository,
            CheckResultRepository checkResultRepository
    ) {
        this.monitorService = monitorService;
        this.siteRepository = siteRepository;
        this.checkResultRepository = checkResultRepository;
    }

    @Transactional
    @Scheduled(fixedDelay = 60000, initialDelay = 5000)
    public void monitorSites() {

        log.info("Starting monitoring cycle");

        for (String url : MonitoredSites.SITES) {

            try {
                SiteCheckResponse result =
                        monitorService.checkSite(url);

                Site site = siteRepository.findByUrl(url)
                        .orElseGet(() -> siteRepository.save(
                                new Site(url, url)
                        ));

                CheckResult checkResult = new CheckResult(
                        site,
                        result.online(),
                        result.statusCode(),
                        result.responseTimeMs(),
                        result.message()
                );

                checkResultRepository.save(checkResult);

                log.info(
                        "{} | {} | HTTP {} | {} ms | {}",
                        url,
                        result.online() ? "ONLINE" : "OFFLINE",
                        result.statusCode(),
                        result.responseTimeMs(),
                        result.message()
                );

            } catch (Exception e) {
                log.error("Failed to save monitoring result for {}", url, e);
            }
        }

        log.info("Monitoring cycle completed");
    }
}
