package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

public class LinkBlocker {
    private final ConfigManager configManager;
    private FileConfiguration moderationConfig;
    private File moderationFile;

    public LinkBlocker(ConfigManager configManager) {
        this.configManager = configManager;
        loadModerationConfig();
    }

    private void loadModerationConfig() {
        moderationFile = new File(configManager.getPlugin().getDataFolder(), "moderation.yml");
        if (!moderationFile.exists()) {
            configManager.getPlugin().saveResource("moderation.yml", false);
        }
        moderationConfig = YamlConfiguration.loadConfiguration(moderationFile);
    }

    public void reloadModeration() {
        loadModerationConfig();
    }

    public boolean isLinkBlockingEnabled() {
        return moderationConfig.getBoolean("link-blocking.enabled", true);
    }

    public boolean isIPBlockingEnabled() {
        return moderationConfig.getBoolean("ip-blocking.enabled", true);
    }

    public String getBlockedMessage() {
        return moderationConfig.getString("blocked-message", "&cСсылки и IP адреса запрещены!");
    }

    public boolean containsLinks(String message) {
        if (!isLinkBlockingEnabled()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        
        if (Pattern.compile("https?://").matcher(lowerMessage).find()) {
            return true;
        }

        if (lowerMessage.contains("www.")) {
            return true;
        }

        return false;
    }

    public boolean containsIP(String message) {
        if (!isIPBlockingEnabled()) {
            return false;
        }

        String lowerMessage = message.toLowerCase();
        
        if (Pattern.compile("\\b[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}\\b").matcher(lowerMessage).find()) {
            return true;
        }

        if (Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b").matcher(message).find()) {
            return true;
        }

        return false;
    }

    public boolean isBlocked(String message) {
        return containsLinks(message) || containsIP(message);
    }
} 