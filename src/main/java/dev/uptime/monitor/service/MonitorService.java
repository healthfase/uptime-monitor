
package dev.uptime.monitor.service;

import dev.uptime.monitor.dto.SiteCheckResponse;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Service
public class MonitorService {

    private final HttpClient client = HttpClient.newHttpClient();

    public SiteCheckResponse checkSite(String url) {

        long startTime = System.nanoTime();

        try {
            URI uri = URI.create(url);

            if (uri.getHost() == null ||
                    (!"http".equals(uri.getScheme())
                            && !"https".equals(uri.getScheme()))) {

                return new SiteCheckResponse(
                        url, false, 0, 0, "Invalid URL"
                );
            }

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(uri)
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .build();

            HttpResponse<Void> response = client.send(
                    request,
                    HttpResponse.BodyHandlers.discarding()
            );

            long responseTime = (System.nanoTime() - startTime)
                    / 1_000_000;

            int statusCode = response.statusCode();

            boolean online = statusCode >= 200
                    && statusCode < 400;

            return new SiteCheckResponse(
                    url,
                    online,
                    statusCode,
                    responseTime,
                    online ? "OK" : "HTTP Error"
            );

        } catch (IOException e) {

            return new SiteCheckResponse(
                    url, false, 0, 0, "Connection error"
            );

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            return new SiteCheckResponse(
                    url, false, 0, 0, "Request interrupted"
            );

        } catch (IllegalArgumentException e) {

            return new SiteCheckResponse(
                    url, false, 0, 0, "Invalid URL"
            );
        }
    }
}
