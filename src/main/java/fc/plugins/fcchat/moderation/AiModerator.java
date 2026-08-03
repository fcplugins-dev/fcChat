package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.JSONArray;
import org.json.JSONObject;

public class AiModerator {
    private final ConfigManager configManager;
    private final AtomicLong blockedMessagesCount;
    private final Map<UUID, Integer> toxicityScores = new ConcurrentHashMap<UUID, Integer>();
    private ToxicityStatistics statistics;
    private volatile boolean loggerEnabled;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public AiModerator(ConfigManager configManager) {
        this.configManager = configManager;
        this.blockedMessagesCount = new AtomicLong(0L);
        this.loggerEnabled = this.configManager.isAiModeratorLoggerEnabledByDefault();
    }

    public void reload() {
        this.loggerEnabled = this.configManager.isAiModeratorLoggerEnabledByDefault();
    }

    public void setStatistics(ToxicityStatistics statistics) {
        this.statistics = statistics;
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
            return Decision.allow(message, 0);
        }
        if (player.hasPermission("fcchat.bypass")) {
            this.debug("Bypass permission: " + player.getName());
            return Decision.allow(message, 0);
        }
        String apiKey = this.configManager.getAiModeratorApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            this.debug("OpenRouter API key is empty");
            return Decision.allow(message, 0);
        }
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(this.configManager.getAiModeratorEndpoint()).openConnection();
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
            messages.put(new JSONObject().put("role", "system").put("content", this.configManager.getAiModeratorPrompt()));
            messages.put(new JSONObject().put("role", "user").put("content", message));
            payload.put("messages", messages);
            payload.put("temperature", 0);
            try (OutputStream os = connection.getOutputStream();){
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                String errorBody = this.readAllSafe(connection.getErrorStream());
                this.debug("OpenRouter HTTP " + responseCode + ": " + this.trimLog(errorBody));
                return Decision.allow(message, 0);
            }
            String responseBody = AiModerator.readAll(connection.getInputStream());
            JSONObject responseJson = new JSONObject(responseBody);
            JSONArray choices = responseJson.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return Decision.allow(message, 0);
            }
            JSONObject messageObject = choices.getJSONObject(0).optJSONObject("message");
            if (messageObject == null) {
                return Decision.allow(message, 0);
            }
            String content = messageObject.optString("content", "").trim();
            if (content.isEmpty() && messageObject.optJSONArray("content") != null) {
                JSONArray contentArray = messageObject.optJSONArray("content");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < contentArray.length(); ++i) {
                    JSONObject part = contentArray.optJSONObject(i);
                    if (part == null) continue;
                    builder.append(part.optString("text", ""));
                }
                content = builder.toString().trim();
            }
            if (content.isEmpty()) {
                this.debug("AI response content is empty");
                return Decision.allow(message, 0);
            }
            JSONObject moderationResult = AiModerator.parseJsonContent(content);
            if (moderationResult == null) {
                this.debug("Cannot parse moderation JSON: " + this.trimLog(content));
                return Decision.allow(message, 0);
            }
            boolean flagged = moderationResult.optBoolean("flagged", false);
            String reason = moderationResult.optString("reason", "");
            int toxicity = moderationResult.optInt("toxicity", 0);
            if (toxicity < 0) {
                toxicity = 0;
            } else if (toxicity > 100) {
                toxicity = 100;
            }
            if (this.configManager.isAiModeratorToxicityEnabled()) {
                this.toxicityScores.put(player.getUniqueId(), toxicity);
            }
            if (this.statistics != null) {
                this.statistics.record(player, toxicity, message);
            }
            String safeMessage = moderationResult.optString("safe_message", message);
            this.debug("AI flagged=" + flagged + " reason=" + reason + " toxicity=" + toxicity);
            if (!flagged) {
                return Decision.allow(safeMessage, toxicity);
            }
            return Decision.block(reason);
        }
        catch (Exception e) {
            this.debug("AI moderation request failed: " + e.getMessage());
            return Decision.allow(message, 0);
        }
    }

    public JSONObject requestJson(String systemPrompt, String userPrompt) {
        if (!this.configManager.isAiModeratorEnabled()) {
            return null;
        }
        String apiKey = this.configManager.getAiModeratorApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return null;
        }
        try {
            HttpURLConnection connection = (HttpURLConnection)new URL(this.configManager.getAiModeratorEndpoint()).openConnection();
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
            messages.put(new JSONObject().put("role", "system").put("content", systemPrompt));
            messages.put(new JSONObject().put("role", "user").put("content", userPrompt));
            payload.put("messages", messages);
            payload.put("temperature", 0);
            try (OutputStream os = connection.getOutputStream()) {
                os.write(payload.toString().getBytes(StandardCharsets.UTF_8));
            }
            int responseCode = connection.getResponseCode();
            if (responseCode < 200 || responseCode >= 300) {
                return null;
            }
            String responseBody = AiModerator.readAll(connection.getInputStream());
            JSONObject responseJson = new JSONObject(responseBody);
            JSONArray choices = responseJson.optJSONArray("choices");
            if (choices == null || choices.length() == 0) {
                return null;
            }
            JSONObject messageObject = choices.getJSONObject(0).optJSONObject("message");
            if (messageObject == null) {
                return null;
            }
            String content = messageObject.optString("content", "").trim();
            if (content.isEmpty() && messageObject.optJSONArray("content") != null) {
                JSONArray contentArray = messageObject.optJSONArray("content");
                StringBuilder builder = new StringBuilder();
                for (int i = 0; i < contentArray.length(); ++i) {
                    JSONObject part = contentArray.optJSONObject(i);
                    if (part == null) continue;
                    builder.append(part.optString("text", ""));
                }
                content = builder.toString().trim();
            }
            if (content.isEmpty()) {
                return null;
            }
            return AiModerator.parseJsonContent(content);
        }
        catch (Exception e) {
            this.debug("AI request failed: " + e.getMessage());
            return null;
        }
    }

    public void registerBlocked(Player player, String reason) {
        this.blockedMessagesCount.incrementAndGet();
        this.sendAdminLog(player, reason);
    }

    public long getBlockedMessagesCount() {
        return this.blockedMessagesCount.get();
    }

    public Integer getToxicity(UUID uuid) {
        return this.toxicityScores.get(uuid);
    }

    public boolean isToxicityEnabled() {
        return this.configManager.isAiModeratorToxicityEnabled();
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
        String safeReason = reason == null || reason.trim().isEmpty() ? "\u041f\u0440\u0438\u0447\u0438\u043d\u0430 \u043d\u0435 \u0443\u043a\u0430\u0437\u0430\u043d\u0430" : reason;
        String baseMessage = this.configManager.getAiModeratorLoggerMessage().replace("{player_name}", player.getName()).replace("{reason}", safeReason);
        String hoverMessage = this.configManager.getAiModeratorLoggerHoverMessage().replace("{player_name}", player.getName()).replace("{reason}", safeReason);
        Component component = LEGACY.deserialize(HexUtils.translateAlternateColorCodes(baseMessage))
            .hoverEvent(HoverEvent.showText(LEGACY.deserialize(HexUtils.translateAlternateColorCodes(hoverMessage))));
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (!online.hasPermission(permission)) continue;
            online.sendMessage(component);
        }
    }

    private static String readAll(InputStream inputStream) throws Exception {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));){
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
        }
        catch (Exception exception) {
            try {
                String cleaned = content.replace("```json", "").replace("```", "").trim();
                return new JSONObject(cleaned);
            }
            catch (Exception cleaned) {
                try {
                    int start = content.indexOf(123);
                    int end = content.lastIndexOf(125);
                    if (start >= 0 && end > start) {
                        return new JSONObject(content.substring(start, end + 1));
                    }
                }
                catch (Exception start) {
                    // empty catch block
                }
                String lower = content.toLowerCase(Locale.ROOT);
                if (lower.contains("\"flagged\": true") || lower.contains("\"flagged\" : true")) {
                    try {
                        return new JSONObject("{\"flagged\":true}");
                    }
                    catch (Exception exception2) {
                        // empty catch block
                    }
                }
                if (lower.contains("\"flagged\": false") || lower.contains("\"flagged\" : false")) {
                    try {
                        return new JSONObject("{\"flagged\":false}");
                    }
                    catch (Exception exception3) {
                        // empty catch block
                    }
                }
                return null;
            }
        }
    }

    private String readAllSafe(InputStream inputStream) {
        if (inputStream == null) {
            return "";
        }
        try {
            return AiModerator.readAll(inputStream);
        }
        catch (Exception ignored) {
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
        private final String safeMessage;
        private final int toxicity;

        private Decision(boolean blocked, String reason, String safeMessage, int toxicity) {
            this.blocked = blocked;
            this.reason = reason == null ? "" : reason;
            this.safeMessage = safeMessage;
            this.toxicity = toxicity;
        }

        public static Decision allow(String safeMessage, int toxicity) {
            return new Decision(false, "", safeMessage, toxicity);
        }

        public static Decision block(String reason) {
            return new Decision(true, reason, null, 0);
        }

        public boolean isBlocked() {
            return this.blocked;
        }

        public String getReason() {
            return this.reason;
        }

        public String getSafeMessage() {
            return this.safeMessage;
        }

        public int getToxicity() {
            return this.toxicity;
        }
    }
}
