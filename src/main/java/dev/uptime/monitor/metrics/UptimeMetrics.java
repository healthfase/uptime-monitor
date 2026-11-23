package dev.uptime.monitor.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

@Component("websiteMonitoringMetrics")
public class UptimeMetrics {

    private final AtomicInteger totalSites = new AtomicInteger(0);
    private final AtomicInteger onlineSites = new AtomicInteger(0);
    private final AtomicInteger offlineSites = new AtomicInteger(0);

    private final Counter checksTotal;
    private final Timer checkDuration;

    public UptimeMetrics(MeterRegistry registry) {

        registry.gauge(
                "uptime_monitor_sites_total",
                totalSites
        );

        registry.gauge(
                "uptime_monitor_sites_online",
                onlineSites
        );

        registry.gauge(
                "uptime_monitor_sites_offline",
                offlineSites
        );

        checksTotal = Counter.builder("uptime_monitor_checks")
                .description("Total number of website checks")
                .register(registry);

        checkDuration = Timer.builder("uptime_monitor_check_duration")
                .description("Time spent checking websites")
                .register(registry);
    }

    public void setTotalSites(int count) {
        totalSites.set(count);
    }

    public void setOnlineSites(int count) {
        onlineSites.set(count);
    }

    public void setOfflineSites(int count) {
        offlineSites.set(count);
    }

    public void incrementChecks() {
        checksTotal.increment();
    }

    public void recordCheckDuration(long milliseconds) {
        checkDuration.record(milliseconds, TimeUnit.MILLISECONDS);
    }
}