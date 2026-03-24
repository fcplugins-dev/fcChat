package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.manager.config.ConfigManager;
import java.io.File;
import java.util.regex.Pattern;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class LinkBlocker {
    private final ConfigManager configManager;
    private FileConfiguration moderationConfig;
    private File moderationFile;

    public LinkBlocker(ConfigManager configManager) {
        this.configManager = configManager;
        this.loadModerationConfig();
    }

    private void loadModerationConfig() {
        this.moderationFile = new File(this.configManager.getPlugin().getDataFolder(), "moderation.yml");
        if (!this.moderationFile.exists()) {
            this.configManager.getPlugin().saveResource("moderation.yml", false);
        }
        this.moderationConfig = YamlConfiguration.loadConfiguration(this.moderationFile);
    }

    public void reloadModeration() {
        this.loadModerationConfig();
    }

    public boolean isLinkBlockingEnabled() {
        return this.moderationConfig.getBoolean("link-blocking.enabled");
    }

    public boolean isIPBlockingEnabled() {
        return this.moderationConfig.getBoolean("ip-blocking.enabled");
    }

    public String getBlockedMessage() {
        return this.moderationConfig.getString("blocked-message");
    }

    public boolean containsLinks(String message) {
        if (!this.isLinkBlockingEnabled()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        if (Pattern.compile("https?://").matcher(lowerMessage).find()) {
            return true;
        }
        return lowerMessage.contains("www.");
    }

    public boolean containsIP(String message) {
        if (!this.isIPBlockingEnabled()) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        if (Pattern.compile("\\b[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b").matcher(lowerMessage).find()) {
            return true;
        }
        return Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b").matcher(message).find();
    }

    public boolean isBlocked(String message) {
        return this.containsLinks(message) || this.containsIP(message);
    }

    public boolean isBlocked(String message, Player player) {
        if (player.hasPermission("fcchat.bypass")) {
            return false;
        }
        return this.containsLinks(message) || this.containsIP(message);
    }
}
