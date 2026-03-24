package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicLong;

public class AiModerator {
    private final ConfigManager configManager;
    private final AtomicLong blockedMessagesCount;
    private volatile boolean loggerEnabled;

    public AiModerator(ConfigManager configManager) {
        this.configManager = configManager;
        this.blockedMessagesCount = new AtomicLong(0L);
        this.loggerEnabled = this.configManager.isAiModeratorLoggerEnabledByDefault();
    }

    public void reload() {
        this.loggerEnabled = this.configManager.isAiModeratorLoggerEnabledByDefault();
    }

    public boolean isActiveFor(Player player) {
        if (!this.configManager.isAiModeratorEnabled()) {
            return false;
        }
        if (player.hasPermission("fcchat.bypass")) {
            return false;
        }
        String apiKey = this.configManager.getAiModeratorApiKey();
        return apiKey != null && !apiKey.trim().isEmpty();
    }

    public Decision moderate(Player player, String message) {
        if (!this.configManager.isAiModeratorEnabled()) {
            this.debug("AI moderator disabled in config");
            return Decision.allow();
        }

        if (player.hasPermission("fcchat.bypass")) {
            this.debug("Bypass permission: " + player.getName());
            return Decision.allow();
        }

        String apiKey = this.configManager.getAiModeratorApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            this.debug("OpenRouter API key is empty");
            return Decision.allow();
        }

        try {
            HttpURLConnection connection = (HttpURLConnection) new URL(this.configManager.getAiModeratorEndpoint()).openConnection();
            connection.setRequestMethod("POST");
            connection.setDoOutput(true);
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(7000);
            connection.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            connection.setRequestProperty("Authorization", "Bearer " + apiKey);
            connection.setRequestProperty("HTTP-Referer", "https://github.com/fcplugins-dev/fcchat");
            connection.setRequestProperty("X-Title", "fcChat");

            JSONObject payload = new JSONObject();
            payload.put("model", this.configManager.getAiModeratorModel());

            JSONArray messages = new JSONArray();
            messages.put(new JSONObject()
                    .put("role", "system")
                    .put("content", this.configManager.getAiModeratorPrompt()));
            messages.put(new JSONObject()
                    .put("role", "user")
                    .put("content", message));

            payload.put("messages", messages);
            payload.put("temperature", 0);

            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }

            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                String errorBody = readAllSafe(connection.getErrorStream());
                this.debug("OpenRouter HTTP " + responseCode + ": " + trimLog(errorBody));
                return Decision.allow();
            }

            String responseBody = readAll(connection.getInputStream());
            JSONObject responseJson = new JSONObject(responseBody);
            JSONArray choices = responseJson.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return Decision.allow();
            }

            JSONObject messageObject = choices.getJSONObject(0).optJSONObject("message");
            if (messageObject == null) {
                return Decision.allow();
            }

            String content = messageObject.optString("content", "").trim();
            if (content.isEmpty() && messageObject.optJSONArray("content") != null) {
                JSONArray contentArray = messageObject.optJSONArray("content");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < contentArray.length(); i++) {
                    JSONObject part = contentArray.optJSONObject(i);
                    if (part != null) {
                        builder.append(part.optString("text", ""));
                    }
                }
                content = builder.toString().trim();
            }

            if (content.isEmpty()) {
                this.debug("AI response content is empty");
                return Decision.allow();
            }

            JSONObject moderationResult = parseJsonContent(content);
            if (moderationResult == null) {
                this.debug("Cannot parse moderation JSON: " + trimLog(content));
                return Decision.allow();
            }

            boolean flagged = moderationResult.optBoolean("flagged", false);
            String reason = moderationResult.optString("reason", "");
            this.debug("AI flagged=" + flagged + " reason=" + reason);

            if (!flagged) {
                return Decision.allow();
            }

            return Decision.block(reason);
        } catch (Exception e) {
            this.debug("AI moderation request failed: " + e.getMessage());
            return Decision.allow();
        }
    }

    public void registerBlocked(Player player, String reason) {
        this.blockedMessagesCount.incrementAndGet();
        this.sendAdminLog(player, reason);
    }

    public long getBlockedMessagesCount() {
        return this.blockedMessagesCount.get();
    }

    public boolean isLoggerEnabled() {
        return this.loggerEnabled;
    }

    public boolean toggleLogger() {
        this.loggerEnabled = !this.loggerEnabled;
        return this.loggerEnabled;
    }

    public String getBlockedMessage() {
        return HexUtils.translateAlternateColorCodes(this.configManager.getAiModeratorBlockedMessage());
    }

    private void sendAdminLog(Player player, String reason) {
        if (!this.loggerEnabled) {
            return;
        }

        String permission = this.configManager.getAiModeratorLoggerPermission();
        String safeReason = (reason == null || reason.trim().isEmpty()) ? "Причина не указана" : reason;

        String baseMessage = this.configManager.getAiModeratorLoggerMessage()
                .replace("{player_name}", player.getName())
                .replace("{reason}", safeReason);
        String hoverMessage = this.configManager.getAiModeratorLoggerHoverMessage()
                .replace("{player_name}", player.getName())
                .replace("{reason}", safeReason);

        TextComponent component = new TextComponent(TextComponent.fromLegacyText(HexUtils.translateAlternateColorCodes(baseMessage)));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(HexUtils.translateAlternateColorCodes(hoverMessage)).create()));

        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.hasPermission(permission)) {
                online.spigot().sendMessage(component);
            }
        }
    }

    private static String readAll(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private static JSONObject parseJsonContent(String content) {
        try {
            return new JSONObject(content);
        } catch (Exception ignored) {
        }

        try {
            String cleaned = content
                    .replace("```json", "")
                    .replace("```", "")
                    .trim();
            return new JSONObject(cleaned);
        } catch (Exception ignored) {
        }

        try {
            int start = content.indexOf('{');
            int end = content.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return new JSONObject(content.substring(start, end + 1));
            }
        } catch (Exception ignored) {
        }

        String lower = content.toLowerCase(Locale.ROOT);
        if (lower.contains("\"flagged\": true") || lower.contains("\"flagged\" : true")) {
            try {
                return new JSONObject("{\"flagged\":true}");
            } catch (Exception ignored) {
            }
        }

        if (lower.contains("\"flagged\": false") || lower.contains("\"flagged\" : false")) {
            try {
                return new JSONObject("{\"flagged\":false}");
            } catch (Exception ignored) {
            }
        }

        return null;
    }

    private String readAllSafe(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try {
            return readAll(inputStream);
        } catch (Exception ignored) {
            return "";
        }
    }

    private String trimLog(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replace('\n', ' ').replace('\r', ' ').trim();
        if (cleaned.length() > 220) {
            return cleaned.substring(0, 220) + "...";
        }
        return cleaned;
    }

    private void debug(String message) {
        if (!this.configManager.isAiModeratorDebugEnabled()) {
            return;
        }
        this.configManager.getPlugin().getLogger().info("[AI-Moderator] " + message);
    }

    public static class Decision {
        private final boolean blocked;
        private final String reason;

        private Decision(boolean blocked, String reason) {
            this.blocked = blocked;
            this.reason = reason == null ? "" : reason;
        }

        public static Decision allow() {
            return new Decision(false, "");
        }

        public static Decision block(String reason) {
            return new Decision(true, reason);
        }

        public boolean isBlocked() {
            return blocked;
        }

        public String getReason() {
            return reason;
        }
    }
}
