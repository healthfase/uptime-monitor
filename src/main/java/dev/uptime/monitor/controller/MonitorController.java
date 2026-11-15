
package dev.uptime.monitor.controller;

import dev.uptime.monitor.dto.SiteCheckResponse;
import dev.uptime.monitor.service.MonitorService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MonitorController {

    private final MonitorService monitorService;

    public MonitorController(MonitorService monitorService) {
        this.monitorService = monitorService;
    }

    @GetMapping("/api/check")
    public SiteCheckResponse checkSite(
            @RequestParam String url) {

        return monitorService.checkSite(url);
    }
}
