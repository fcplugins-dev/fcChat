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

    public boolean isChatEnabled() {
        return config.getBoolean("chat.enabled", true);
    }

    public int getLocalChatRadius() {
        return config.getInt("local-chat.radius", 100);
    }

    public String getChatPrefix() {
        return config.getString("chat.prefix", "!");
    }

    public String getLocalChatFormat() {
        return config.getString("local-chat.format", "&7[&bЛокальный&7] &f{player}: &7{message}");
    }

    public String getGlobalChatFormat() {
        return config.getString("global-chat.format", "&7[&aГлобальный&7] &f{player}: &7{message}");
    }

    public String getCopyPermission() {
        return config.getString("copy.permission", "fcchat.copy");
    }

    public boolean isColorChatEnabled() {
        return config.getBoolean("color-chat.enabled", true);
    }

    public String getColorChatPermission() {
        return config.getString("color-chat.permission", "fcchat.color");
    }

    public boolean isHiddenTextEnabled() {
        return config.getBoolean("hidden-text.enabled", true);
    }

    public String getHiddenTextPermission() {
        return config.getString("hidden-text.permission", "fcchat.hidden");
    }

    public String getHiddenTextSymbol() {
        return config.getString("hidden-text.symbol", "█");
    }

    public int getHiddenTextLength() {
        return config.getInt("hidden-text.length", 7);
    }

    public boolean isCopyEnabled() {
        return config.getBoolean("copy.enabled", true);
    }

    public boolean isAntiSpamEnabled() {
        return moderation.getBoolean("anti-spam.enabled", true);
    }

    public double getAntiSpamCooldown() {
        return moderation.getDouble("anti-spam.cooldown", 3.0);
    }

    public String getAntiSpamMessage() {
        return moderation.getString("anti-spam.message", "&cНе спамьте! Подождите {time} секунд.");
    }

    public boolean isNewPlayerChatEnabled() {
        return moderation.getBoolean("new-player-chat.enabled", true);
    }

    public int getNewPlayerBlockTime() {
        return moderation.getInt("new-player-chat.block-time", 300);
    }

    public String getNewPlayerMessage() {
        return moderation.getString("new-player-chat.message", "&cЧат заблокирован для новых игроков на {time} секунд.");
    }

    public String getAntiSpamBypassPermission() {
        return moderation.getString("anti-spam.bypass-permission", "fcchat.antispam.bypass");
    }

    public String getNewPlayerBypassPermission() {
        return moderation.getString("new-player-chat.bypass-permission", "fcchat.newplayer.bypass");
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
        return config.getBoolean("discord.enabled", false);
    }

    public String getDiscordWebhookUrl() {
        return config.getString("discord.webhook-url", "");
    }

    public String getDiscordMessageFormat() {
        return config.getString("discord.message-format", "{player}: {message}");
    }

    public boolean isSpyEnabled() {
        return config.getBoolean("spy.enabled", true);
    }

    public String getSpyPermission() {
        return config.getString("spy.permission", "fcchat.spy");
    }

    public String getSpyMessageFormat() {
        return config.getString("spy.message-format", "&8[&cSPY&8] &7{formatted}");
    }

    public boolean isPlayerInfoEnabled() {
        return config.getBoolean("player-info.enabled", true);
    }

    public String getPlayerInfoPermission() {
        return config.getString("player-info.permission", "fcchat.info");
    }

    public List<String> getPlayerInfoLines() {
        return config.getStringList("player-info.lines");
    }

    public FcChat getPlugin() {
        return plugin;
    }
} 