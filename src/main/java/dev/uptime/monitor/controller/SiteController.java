
package dev.uptime.monitor.controller;

import dev.uptime.monitor.entity.Site;
import dev.uptime.monitor.service.SiteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import dev.uptime.monitor.dto.SiteStatisticsResponse;
import java.util.List;
import dev.uptime.monitor.dto.CheckHistoryResponse;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/sites")
public class    SiteController {
    // GET /api/sites/{id}/history
    @GetMapping("/{id}/history")
    public ResponseEntity<?> getSiteHistory(
            @PathVariable Long id,
            @RequestParam(defaultValue = "50") int limit
    ) {

        try {
            List<CheckHistoryResponse> history =
                    siteService.getSiteHistory(id, limit);

            return ResponseEntity.ok(history);

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
    // GET /api/sites/{id}/statistics
    @GetMapping("/{id}/statistics")
    public SiteStatisticsResponse getSiteStatistics(@PathVariable Long id) {
        return siteService.getSiteStatistics(id);
    }

    private final SiteService siteService;

    public SiteController(SiteService siteService) {
        this.siteService = siteService;
    }

    // GET /api/sites
    @GetMapping
    public List<Site> getAllSites() {
        return siteService.getAllSites();
    }

    // GET /api/sites/{id}
    @GetMapping("/{id}")
    public Site getSiteById(@PathVariable Long id) {
        return siteService.getSiteById(id);
    }

    // POST /api/sites
    @PostMapping
    public ResponseEntity<?> addSite(
            @Valid @RequestBody CreateSiteRequest request
    ) {

        try {
            Site site = siteService.addSite(
                    request.url(),
                    request.name()
            );

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(site);

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .badRequest()
                    .body(e.getMessage());
        }
    }

    // DELETE /api/sites/{id}
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSite(@PathVariable Long id) {

        try {
            siteService.deleteSite(id);
            return ResponseEntity.noContent().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(e.getMessage());
        }
    }

    // DTO для создания сайта
    public record CreateSiteRequest(

            @jakarta.validation.constraints.NotBlank(
                    message = "URL cannot be empty"
            )
            String url,

            @jakarta.validation.constraints.NotBlank(
                    message = "Name cannot be empty"
            )
            String name

    ) {}
}
