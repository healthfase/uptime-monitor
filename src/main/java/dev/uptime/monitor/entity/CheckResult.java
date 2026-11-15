
package dev.uptime.monitor.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "check_results")
public class CheckResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "site_id", nullable = false)
    private Site site;

    private boolean online;

    private int statusCode;

    private long responseTimeMs;

    private String message;

    private Instant checkedAt;

    public CheckResult() {
    }

    public CheckResult(
            Site site,
            boolean online,
            int statusCode,
            long responseTimeMs,
            String message
    ) {
        this.site = site;
        this.online = online;
        this.statusCode = statusCode;
        this.responseTimeMs = responseTimeMs;
        this.message = message;
        this.checkedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Site getSite() {
        return site;
    }

    public boolean isOnline() {
        return online;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public long getResponseTimeMs() {
        return responseTimeMs;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCheckedAt() {
        return checkedAt;
    }
}
