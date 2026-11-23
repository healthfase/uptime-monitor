package dev.uptime.monitor.notification;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class TelegramNotifier {

    @Value("${TELEGRAM_BOT_TOKEN:}")
    private String botToken;

    @Value("${TELEGRAM_CHAT_ID:}")
    private String chatId;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public void sendMessage(String message) {

        if (botToken.isBlank() || chatId.isBlank()) {
            return;
        }

        try {
            String url = "https://api.telegram.org/bot"
                    + botToken
                    + "/sendMessage";

            String json = """
                    {
                      "chat_id": "%s",
                      "text": "%s"
                    }
                    """.formatted(
                    chatId,
                    escapeJson(message)
            );

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(json))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString()
            );

            if (response.statusCode() != 200) {
                System.err.println(
                        "Telegram error: " + response.body()
                );
            }

        } catch (Exception e) {
            System.err.println(
                    "Failed to send Telegram notification: "
                            + e.getMessage()
            );
        }
    }

    private String escapeJson(String text) {
        return text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
    }
}