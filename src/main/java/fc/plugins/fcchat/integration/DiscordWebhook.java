package fc.plugins.fcchat.integration;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.manager.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;

public class DiscordWebhook {
    private final FcChat plugin;
    private final ConfigManager configManager;
    private boolean isEnabled = false;
    private String webhookUrl;
    private final ConcurrentLinkedQueue<WebhookMessage> messageQueue;
    private final AtomicLong lastSendTime;
    private static final long MIN_DELAY_MS = 500;
    private volatile boolean processing = false;

    public DiscordWebhook(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.messageQueue = new ConcurrentLinkedQueue<>();
        this.lastSendTime = new AtomicLong(0);
        this.setupWebhook();
        this.startQueueProcessor();
    }

    private void setupWebhook() {
        this.isEnabled = this.configManager.isDiscordWebhookEnabled();
        if (this.isEnabled) {
            this.webhookUrl = this.configManager.getDiscordWebhookUrl();
            if (this.webhookUrl == null || this.webhookUrl.isEmpty()) {
                this.isEnabled = false;
            }
        }
    }

    public boolean isEnabled() {
        return this.isEnabled && this.webhookUrl != null && !this.webhookUrl.isEmpty();
    }

    private void startQueueProcessor() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
            if (processing || !isEnabled() || messageQueue.isEmpty()) {
                return;
            }
            
            processing = true;
            try {
                long currentTime = System.currentTimeMillis();
                long timeSinceLastSend = currentTime - lastSendTime.get();
                
                if (timeSinceLastSend < MIN_DELAY_MS) {
                    return;
                }
                
                WebhookMessage msg = messageQueue.poll();
                if (msg != null) {
                    sendWebhookMessage(msg);
                    lastSendTime.set(System.currentTimeMillis());
                }
            } finally {
                processing = false;
            }
        }, 20L, 10L);
    }
    
    public void sendMessageToDiscord(Player player, String message) {
        if (!this.isEnabled()) {
            return;
        }
        
        String cleanMessage = ChatColor.stripColor(message);
        if (cleanMessage.length() > 2000) {
            cleanMessage = cleanMessage.substring(0, 1997) + "...";
        }
        
        WebhookMessage webhookMessage = new WebhookMessage(player.getName(), cleanMessage);
        messageQueue.offer(webhookMessage);
    }
    
    private void sendWebhookMessage(WebhookMessage msg) {
        try {
            String username = this.configManager.getDiscordWebhookUsername();
            String avatarUrl = this.configManager.getDiscordWebhookAvatarUrl();
            String jsonPayload = buildJsonPayload(msg.playerName, msg.message, username, avatarUrl);
            
            URL url = new URL(this.webhookUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
            connection.setRequestProperty("User-Agent", "FcChat-Webhook");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setDoOutput(true);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonPayload.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            if (responseCode == 429) {
                messageQueue.offer(msg);
            } else if (responseCode < 200 || responseCode >= 300) {
                this.plugin.getLogger().warning("Discord webhook failed with response code: " + responseCode);
            }
            
            connection.disconnect();
        } catch (Exception e) {
            this.plugin.getLogger().warning("Failed to send message to Discord webhook: " + e.getMessage());
        }
    }

    private String buildJsonPayload(String playerName, String message, String username, String avatarUrl) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        if (username != null && !username.isEmpty()) {
            json.append("\"username\":\"").append(escapeJson(username)).append("\",");
        }
        
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            json.append("\"avatar_url\":\"").append(escapeJson(avatarUrl)).append("\",");
        }
        
        json.append("\"content\":\"**").append(escapeJson(playerName)).append("**: ").append(escapeJson(message)).append("\"");
        json.append("}");
        
        return json.toString();
    }

    private String escapeJson(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }

    public void reload() {
        this.setupWebhook();
    }

    public void disable() {
        this.isEnabled = false;
        this.messageQueue.clear();
    }
    
    private static class WebhookMessage {
        final String playerName;
        final String message;
        
        WebhookMessage(String playerName, String message) {
            this.playerName = playerName;
            this.message = message;
        }
    }
}
