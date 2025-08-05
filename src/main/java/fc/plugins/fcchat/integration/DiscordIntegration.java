package fc.plugins.fcchat.integration;

import fc.plugins.fcchat.config.ConfigManager;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class DiscordIntegration {
    private final ConfigManager configManager;
    private final HttpClient httpClient;
    private boolean isEnabled = false;
    private String webhookUrl = "";

    public DiscordIntegration(ConfigManager configManager) {
        this.configManager = configManager;
        this.httpClient = HttpClient.newHttpClient();
        setupDiscord();
    }

    private void setupDiscord() {
        this.isEnabled = configManager.isDiscordEnabled();
        this.webhookUrl = configManager.getDiscordWebhookUrl();
    }

    public boolean isEnabled() {
        return isEnabled && !webhookUrl.isEmpty();
    }

    public void sendMessage(Player player, String message) {
        if (!isEnabled()) {
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                String discordMessage = formatDiscordMessage(player, message);
                sendWebhookMessage(discordMessage);
            } catch (Exception e) {
            }
        });
    }

    private String formatDiscordMessage(Player player, String message) {
        String format = configManager.getDiscordMessageFormat();
        return format
                .replace("{player}", player.getName())
                .replace("{message}", message);
    }

    private void sendWebhookMessage(String content) throws IOException, InterruptedException {
        String jsonPayload = "{\"content\":\"" + escapeJson(content) + "\"}";
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(webhookUrl))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        
        if (response.statusCode() != 204) {
        }
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    public void reload() {
        setupDiscord();
    }
} 