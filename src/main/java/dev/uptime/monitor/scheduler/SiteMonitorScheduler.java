
package dev.uptime.monitor.scheduler;

import dev.uptime.monitor.dto.SiteCheckResponse;
import dev.uptime.monitor.entity.Site;
import dev.uptime.monitor.entity.CheckResult;
import dev.uptime.monitor.metrics.UptimeMetrics;
import dev.uptime.monitor.notification.TelegramNotifier;
import dev.uptime.monitor.repository.SiteRepository;
import dev.uptime.monitor.repository.CheckResultRepository;
import dev.uptime.monitor.service.MonitorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class SiteMonitorScheduler {

    private static final Logger log =
            LoggerFactory.getLogger(SiteMonitorScheduler.class);

    private final MonitorService monitorService;
    private final SiteRepository siteRepository;
    private final CheckResultRepository checkResultRepository;
    private final UptimeMetrics uptimeMetrics;
    private final TelegramNotifier telegramNotifier;

    public SiteMonitorScheduler(
            MonitorService monitorService,
            SiteRepository siteRepository,
            CheckResultRepository checkResultRepository,
            UptimeMetrics uptimeMetrics,
            TelegramNotifier telegramNotifier
    ) {
        this.monitorService = monitorService;
        this.siteRepository = siteRepository;
        this.checkResultRepository = checkResultRepository;
        this.uptimeMetrics = uptimeMetrics;
        this.telegramNotifier = telegramNotifier;
    }

    @Transactional
    @Scheduled(fixedDelay = 60000, initialDelay = 5000)
    public void monitorSites() {

        log.info("Starting monitoring cycle");

        // Получаем сайты из PostgreSQL
        List<Site> sites = siteRepository.findAll();

        int onlineCount = 0;
        int offlineCount = 0;

        // Обновляем общее количество сайтов
        uptimeMetrics.setTotalSites(sites.size());

        // Проверяем каждый сайт из базы
        for (Site site : sites) {

            String url = site.getUrl();

            long startTime = System.nanoTime();

            SiteCheckResponse result;

            try {
                result = monitorService.checkSite(url);

            } catch (Exception e) {

                long duration =
                        (System.nanoTime() - startTime) / 1_000_000;

                uptimeMetrics.incrementChecks();
                uptimeMetrics.recordCheckDuration(duration);

                log.error("Failed to check site {}", url, e);
                continue;
            }

            long duration =
                    (System.nanoTime() - startTime) / 1_000_000;

            uptimeMetrics.incrementChecks();
            uptimeMetrics.recordCheckDuration(duration);

            if (result.online()) {
                onlineCount++;
            } else {
                offlineCount++;
            }

            try {

                // Получаем предыдущую проверку до сохранения текущей
                var previousResult =
                        checkResultRepository
                                .findTopBySiteOrderByCheckedAtDesc(site);

                // Уведомляем только при изменении статуса
                if (previousResult.isPresent()) {

                    boolean wasOnline =
                            previousResult.get().isOnline();

                    boolean isOnline = result.online();

                    if (wasOnline && !isOnline) {

                        telegramNotifier.sendMessage(
                                "🔴 SITE DOWN\n\n"
                                        + "Сайт: " + url + "\n"
                                        + "HTTP: " + result.statusCode() + "\n"
                                        + "Причина: " + result.message() + "\n"
                                        + "Время ответа: "
                                        + result.responseTimeMs() + " ms"
                        );

                    } else if (!wasOnline && isOnline) {

                        telegramNotifier.sendMessage(
                                "🟢 SITE RECOVERED\n\n"
                                        + "Сайт: " + url + "\n"
                                        + "HTTP: " + result.statusCode() + "\n"
                                        + "Время ответа: "
                                        + result.responseTimeMs() + " ms"
                        );
                    }
                }

                // Сохраняем результат проверки в PostgreSQL
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
                log.error(
                        "Failed to save monitoring result for {}",
                        url,
                        e
                );
            }
        }

        // Обновляем метрики после завершения цикла
        uptimeMetrics.setOnlineSites(onlineCount);
        uptimeMetrics.setOfflineSites(offlineCount);

        log.info(
                "Monitoring cycle completed. Total: {}, Online: {}, Offline: {}",
                sites.size(),
                onlineCount,
                offlineCount
        );
    }
}
