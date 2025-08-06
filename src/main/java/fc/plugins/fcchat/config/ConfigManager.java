package fc.plugins.fcchat.config;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.integration.LuckPermsIntegration;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import fc.plugins.fcchat.integration.DiscordIntegration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.List;

public class ConfigManager {
    private final FcChat plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration moderation;
    private File messagesFile;
    private File moderationFile;
    private LuckPermsIntegration luckPermsIntegration;
    private PlaceholderAPIIntegration placeholderAPI;
    private DiscordIntegration discordIntegration;

    public ConfigManager(FcChat plugin) {
        this.plugin = plugin;
        loadConfig();
        loadMessages();
        loadModeration();
        luckPermsIntegration = new LuckPermsIntegration();
        placeholderAPI = new PlaceholderAPIIntegration();
        discordIntegration = new DiscordIntegration(this);
    }

    public void loadConfig() {
        plugin.saveDefaultConfig();
        config = plugin.getConfig();
    }

    public void loadMessages() {
        messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        if (!messagesFile.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        messages = YamlConfiguration.loadConfiguration(messagesFile);
    }

    public void loadModeration() {
        moderationFile = new File(plugin.getDataFolder(), "moderation.yml");
        if (!moderationFile.exists()) {
            plugin.saveResource("moderation.yml", false);
        }
        moderation = YamlConfiguration.loadConfiguration(moderationFile);
    }

    public String getMessage(String path) {
        return messages.getString(path);
    }

    public void reloadConfig() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadMessages();
        loadModeration();
        
        if (discordIntegration != null) {
            discordIntegration.reload();
        }
        
        if (plugin.getChatManager() != null) {
            plugin.getChatManager().reloadModeration();
        }
    }

    public int getLocalChatRadius() {
        return config.getInt("local-chat.radius");
    }

    public String getChatPrefix() {
        return config.getString("chat.prefix");
    }

    public String getLocalChatFormat() {
        return config.getString("local-chat.format");
    }

    public String getGlobalChatFormat() {
        return config.getString("global-chat.format");
    }

    public String getCopyPermission() {
        return config.getString("copy.permission");
    }

    public boolean isColorChatEnabled() {
        return config.getBoolean("color-chat.enabled");
    }

    public String getColorChatPermission() {
        return config.getString("color-chat.permission");
    }

    public boolean isHiddenTextEnabled() {
        return config.getBoolean("hidden-text.enabled");
    }

    public String getHiddenTextPermission() {
        return config.getString("hidden-text.permission");
    }

    public String getHiddenTextSymbol() {
        return config.getString("hidden-text.symbol");
    }

    public int getHiddenTextLength() {
        return config.getInt("hidden-text.length");
    }

    public boolean isCopyEnabled() {
        return config.getBoolean("copy.enabled");
    }

    public boolean isAntiSpamEnabled() {
        return moderation.getBoolean("anti-spam.enabled");
    }

    public double getAntiSpamCooldown() {
        return moderation.getDouble("anti-spam.cooldown");
    }

    public String getAntiSpamMessage() {
        return moderation.getString("anti-spam.message");
    }

    public boolean isNewPlayerChatEnabled() {
        return moderation.getBoolean("new-player-chat.enabled");
    }

    public int getNewPlayerBlockTime() {
        return moderation.getInt("new-player-chat.block-time");
    }

    public String getNewPlayerMessage() {
        return moderation.getString("new-player-chat.message");
    }

    public String getAntiSpamBypassPermission() {
        return moderation.getString("anti-spam.bypass-permission");
    }

    public String getNewPlayerBypassPermission() {
        return moderation.getString("new-player-chat.bypass-permission");
    }



    public LuckPermsIntegration getLuckPermsIntegration() {
        return luckPermsIntegration;
    }

    public PlaceholderAPIIntegration getPlaceholderAPI() {
        return placeholderAPI;
    }

    public DiscordIntegration getDiscordIntegration() {
        return discordIntegration;
    }

    public boolean isDiscordEnabled() {
        return config.getBoolean("discord.enabled");
    }

    public String getDiscordWebhookUrl() {
        return config.getString("discord.webhook-url");
    }

    public String getDiscordMessageFormat() {
        return config.getString("discord.message-format");
    }

    public boolean isSpyEnabled() {
        return config.getBoolean("spy.enabled");
    }

    public String getSpyPermission() {
        return config.getString("spy.permission");
    }

    public String getSpyMessageFormat() {
        return config.getString("spy.message-format");
    }

    public boolean isPlayerInfoEnabled() {
        return config.getBoolean("player-info.enabled");
    }

    public String getPlayerInfoPermission() {
        return config.getString("player-info.permission");
    }

    public List<String> getPlayerInfoLines() {
        return config.getStringList("player-info.lines");
    }

    public FcChat getPlugin() {
        return plugin;
    }

    public boolean updateCheck() {
        return config.getBoolean("update-check");
    }
}