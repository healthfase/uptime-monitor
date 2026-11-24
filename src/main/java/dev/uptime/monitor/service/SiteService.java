package dev.uptime.monitor.service;

import dev.uptime.monitor.dto.CheckHistoryResponse;
import dev.uptime.monitor.entity.CheckResult;
import org.springframework.data.domain.PageRequest;
import dev.uptime.monitor.exception.ResourceNotFoundException;
import dev.uptime.monitor.entity.Site;
import dev.uptime.monitor.repository.SiteRepository;
import org.springframework.stereotype.Service;
import dev.uptime.monitor.repository.CheckResultRepository;
import dev.uptime.monitor.dto.SitePageResponse;
import dev.uptime.monitor.dto.SiteStatisticsResponse;
import java.util.Comparator;
import java.util.Locale;
import java.net.URI;
import java.util.List;

@Service
public class SiteService {
    public SitePageResponse getSitesPage(
            String status,
            String search,
            int page,
            int size,
            String sort,
            String direction
    ) {
        if (page < 0) {
            throw new IllegalArgumentException("Page must be >= 0");
        }

        if (size < 1 || size > 100) {
            throw new IllegalArgumentException("Size must be between 1 and 100");
        }

        if (!sort.equals("name") && !sort.equals("url") && !sort.equals("id")) {
            throw new IllegalArgumentException("Invalid sort field");
        }

        if (!direction.equalsIgnoreCase("asc")
                && !direction.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Direction must be asc or desc");
        }

        if (!status.equalsIgnoreCase("all")
                && !status.equalsIgnoreCase("online")
                && !status.equalsIgnoreCase("offline")) {
            throw new IllegalArgumentException("Status must be all, online or offline");
        }

        List<Site> sites = siteRepository.findAll();

        // Фильтрация по поисковой строке
        if (search != null && !search.isBlank()) {
            String query = search.toLowerCase(Locale.ROOT);

            sites = sites.stream()
                    .filter(site ->
                            (site.getName() != null
                                    && site.getName().toLowerCase(Locale.ROOT).contains(query))
                                    || site.getUrl().toLowerCase(Locale.ROOT).contains(query)
                    )
                    .toList();
        }

        // Фильтрация по статусу
        if (!status.equalsIgnoreCase("all")) {
            boolean onlineFilter = status.equalsIgnoreCase("online");

            sites = sites.stream()
                    .filter(site -> getSiteStatistics(site.getId()).online() == onlineFilter)
                    .toList();
        }

        // Сортировка
        Comparator<Site> comparator;

        switch (sort) {
            case "name" -> comparator = Comparator.comparing(
                    Site::getName,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
            );
            case "url" -> comparator = Comparator.comparing(Site::getUrl);
            case "id" -> comparator = Comparator.comparing(Site::getId);
            default -> throw new IllegalArgumentException("Invalid sort field");
        }

        if (direction.equalsIgnoreCase("desc")) {
            comparator = comparator.reversed();
        }

        sites = sites.stream()
                .sorted(comparator)
                .toList();

        // Пагинация
        long totalElements = sites.size();

        int totalPages = (int) Math.ceil((double) totalElements / size);

        int fromIndex = page * size;

        if (fromIndex >= totalElements) {
            return new SitePageResponse(
                    List.of(),
                    page,
                    size,
                    totalElements,
                    totalPages
            );
        }

        int toIndex = Math.min(fromIndex + size, sites.size());

        List<Site> pageSites = sites.subList(fromIndex, toIndex);

        return new SitePageResponse(
                pageSites,
                page,
                size,
                totalElements,
                totalPages
        );
    }
    private final SiteRepository siteRepository;
    private final CheckResultRepository checkResultRepository;

    public SiteService(
            SiteRepository siteRepository,
            CheckResultRepository checkResultRepository
    ) {
        this.siteRepository = siteRepository;
        this.checkResultRepository = checkResultRepository;
    }

    // Получить все сайты
    public List<Site> getAllSites() {
        return siteRepository.findAll();
    }

    // Найти сайт по ID
    public Site getSiteById(Long id) {
        return siteRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Site not found with id: " + id
                        )
                );
    }

    // Добавить сайт
    public Site addSite(String url, String name) {

        validateUrl(url);

        if (siteRepository.findByUrl(url).isPresent()) {
            throw new IllegalArgumentException(
                    "Site with this URL already exists"
            );
        }

        Site site = new Site(url, name);

        return siteRepository.save(site);
    }

    // Удалить сайт
    public void deleteSite(Long id) {

        if (!siteRepository.existsById(id)) {
            throw new ResourceNotFoundException(
                    "Site not found with id: " + id
            );
        }

        siteRepository.deleteById(id);
    }

    // Проверка URL
    private void validateUrl(String url) {

        if (url == null || url.isBlank()) {
            throw new IllegalArgumentException("URL cannot be empty");
        }

        try {
            URI uri = URI.create(url);

            String scheme = uri.getScheme();

            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http")
                    && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {

                throw new IllegalArgumentException(
                        "URL must start with http:// or https://"
                );
            }

        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid URL: " + url
            );
        }
    }
    public List<CheckHistoryResponse> getSiteHistory(
            Long siteId,
            int limit
    ) {

        Site site = getSiteById(siteId);

        if (limit < 1 || limit > 1000) {
            throw new IllegalArgumentException(
                    "Limit must be between 1 and 1000"
            );
        }

        List<CheckResult> results =
                checkResultRepository.findBySiteOrderByCheckedAtDesc(
                        site,
                        PageRequest.of(0, limit)
                );

        return results.stream()
                .map(result -> new CheckHistoryResponse(
                        result.getId(),
                        result.isOnline(),
                        result.getStatusCode(),
                        result.getResponseTimeMs(),
                        result.getMessage(),
                        result.getCheckedAt()
                ))
                .toList();
    }
    public SiteStatisticsResponse getSiteStatistics(Long siteId) {

        Site site = getSiteById(siteId);

        long totalChecks = checkResultRepository.countBySite(site);

        long successfulChecks =
                checkResultRepository.countBySiteAndOnlineTrue(site);

        long failedChecks =
                checkResultRepository.countBySiteAndOnlineFalse(site);

        double uptimePercentage = 0.0;

        if (totalChecks > 0) {
            uptimePercentage =
                    (successfulChecks * 100.0) / totalChecks;
        }

        Double averageResponseTime =
                checkResultRepository.findAverageResponseTimeBySite(site);

        if (averageResponseTime == null) {
            averageResponseTime = 0.0;
        }

        CheckResult latestResult =
                checkResultRepository
                        .findTopBySiteOrderByCheckedAtDesc(site)
                        .orElse(null);

        boolean online = latestResult != null && latestResult.isOnline();

        int statusCode = latestResult != null
                ? latestResult.getStatusCode()
                : 0;

        long responseTimeMs = latestResult != null
                ? latestResult.getResponseTimeMs()
                : 0;

        return new SiteStatisticsResponse(
                site.getId(),
                site.getUrl(),
                site.getName(),
                online,
                statusCode,
                responseTimeMs,
                totalChecks,
                successfulChecks,
                failedChecks,
                uptimePercentage,
                averageResponseTime
        );
    }
}
