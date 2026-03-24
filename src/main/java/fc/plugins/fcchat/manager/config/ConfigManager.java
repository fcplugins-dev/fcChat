package fc.plugins.fcchat.manager.config;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.integration.LuckPermsIntegration;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager {
    private final FcChat plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration moderation;
    private FileConfiguration privateMessages;
    private File messagesFile;
    private File moderationFile;
    private File privateMessagesFile;
    private LuckPermsIntegration luckPermsIntegration;
    private PlaceholderAPIIntegration placeholderAPI;
    
    private String cachedLocalFormat;
    private String cachedGlobalFormat;
    private String cachedChatPrefix;
    private int cachedLocalRadius;
    private boolean cachedColorChat;
    private boolean cachedHiddenText;
    private boolean cachedHologramEnabled;

    public ConfigManager(FcChat plugin) {
        this.plugin = plugin;
        this.loadConfig();
        this.loadMessages();
        this.loadModeration();
        this.loadPrivateMessages();
        this.luckPermsIntegration = new LuckPermsIntegration();
        this.placeholderAPI = new PlaceholderAPIIntegration();
        this.cacheValues();
    }

    public void loadConfig() {
        this.plugin.saveDefaultConfig();
        this.config = this.plugin.getConfig();
    }

    public void loadMessages() {
        this.messagesFile = new File(this.plugin.getDataFolder(), "messages.yml");
        if (!this.messagesFile.exists()) {
            this.plugin.saveResource("messages.yml", false);
        }
        this.messages = YamlConfiguration.loadConfiguration(this.messagesFile);
    }

    public void loadModeration() {
        this.moderationFile = new File(this.plugin.getDataFolder(), "moderation.yml");
        if (!this.moderationFile.exists()) {
            this.plugin.saveResource("moderation.yml", false);
        }
        this.moderation = YamlConfiguration.loadConfiguration(this.moderationFile);
    }

    public void loadPrivateMessages() {
        this.privateMessagesFile = new File(this.plugin.getDataFolder(), "private-messages.yml");
        if (!this.privateMessagesFile.exists()) {
            this.plugin.saveResource("private-messages.yml", false);
        }
        this.privateMessages = YamlConfiguration.loadConfiguration(this.privateMessagesFile);
    }

    public String getMessage(String path) {
        return this.messages.getString(path);
    }

    private void cacheValues() {
        this.cachedLocalFormat = this.config.getString("local-chat.format");
        this.cachedGlobalFormat = this.config.getString("global-chat.format");
        this.cachedChatPrefix = this.config.getString("chat.prefix");
        this.cachedLocalRadius = this.config.getInt("local-chat.radius");
        this.cachedColorChat = this.config.getBoolean("color-chat.enabled");
        this.cachedHiddenText = this.config.getBoolean("hidden-text.enabled");
        this.cachedHologramEnabled = this.config.getBoolean("hologram-messages.enabled");
    }
    
    public void reloadConfig() {
        this.plugin.reloadConfig();
        this.config = this.plugin.getConfig();
        this.loadMessages();
        this.loadModeration();
        this.loadPrivateMessages();
        this.cacheValues();
        if (this.plugin.getDiscordWebhook() != null) {
            this.plugin.getDiscordWebhook().reload();
        }
        if (this.plugin.getChatManager() != null) {
            this.plugin.getChatManager().reloadModeration();
        }
    }

    public int getLocalChatRadius() {
        return this.cachedLocalRadius;
    }

    public String getChatPrefix() {
        return this.cachedChatPrefix;
    }

    public String getLocalChatFormat() {
        return this.cachedLocalFormat;
    }

    public String getGlobalChatFormat() {
        return this.cachedGlobalFormat;
    }

    public boolean isColorChatEnabled() {
        return this.cachedColorChat;
    }

    public String getColorChatPermission() {
        return this.config.getString("color-chat.permission");
    }

    public boolean isHiddenTextEnabled() {
        return this.cachedHiddenText;
    }

    public String getHiddenTextPermission() {
        return this.config.getString("hidden-text.permission");
    }

    public String getHiddenTextSymbol() {
        return this.config.getString("hidden-text.symbol");
    }

    public int getHiddenTextLength() {
        return this.config.getInt("hidden-text.length");
    }



    public boolean isAntiSpamEnabled() {
        return this.moderation.getBoolean("anti-spam.enabled");
    }

    public double getAntiSpamCooldown() {
        return this.moderation.getDouble("anti-spam.cooldown");
    }

    public String getAntiSpamMessage() {
        return this.moderation.getString("anti-spam.message");
    }

    public boolean isNewPlayerChatEnabled() {
        return this.moderation.getBoolean("new-player-chat.enabled");
    }

    public int getNewPlayerBlockTime() {
        return this.moderation.getInt("new-player-chat.block-time");
    }

    public String getNewPlayerMessage() {
        return this.moderation.getString("new-player-chat.message");
    }

    public String getAntiSpamBypassPermission() {
        return this.moderation.getString("anti-spam.bypass-permission");
    }

    public String getNewPlayerBypassPermission() {
        return this.moderation.getString("new-player-chat.bypass-permission");
    }

    public boolean isAntiCapsEnabled() {
        return this.moderation.getBoolean("anti-caps.enabled", false);
    }

    public int getAntiCapsPercent() {
        return this.moderation.getInt("anti-caps.percent", 50);
    }

    public String getAntiCapsMode() {
        return this.moderation.getString("anti-caps.mode", "lowercase");
    }

    public String getAntiCapsMessage() {
        return this.moderation.getString("anti-caps.message", "&cToo many capital letters!");
    }

    public String getAntiCapsBypassPermission() {
        return this.moderation.getString("anti-caps.bypass-permission", "fcchat.bypass");
    }

    public boolean isAiModeratorEnabled() {
        return this.moderation.getBoolean("ai-moderator.enabled", false);
    }

    public boolean isAiModeratorDebugEnabled() {
        return this.moderation.getBoolean("ai-moderator.debug", false);
    }

    public String getAiModeratorEndpoint() {
        return this.moderation.getString("ai-moderator.endpoint", "https://openrouter.ai/api/v1/chat/completions");
    }

    public String getAiModeratorApiKey() {
        return this.moderation.getString("ai-moderator.api-key", "");
    }

    public String getAiModeratorModel() {
        return this.moderation.getString("ai-moderator.model", "openai/gpt-4o-mini");
    }

    public String getAiModeratorBlockedMessage() {
        return this.moderation.getString("ai-moderator.blocked-message", "&c[⚠] &fСообщение заблокировано &cAI-модератором&f.");
    }

    public boolean isAiModeratorLoggerEnabledByDefault() {
        return this.moderation.getBoolean("ai-moderator.logger.enabled", false);
    }

    public String getAiModeratorLoggerPermission() {
        return this.moderation.getString("ai-moderator.logger.permission", "fcchat.ai.logger");
    }

    public String getAiModeratorLoggerMessage() {
        return this.moderation.getString("ai-moderator.logger.message", "&6[AI] &fСообщение игрока &e{player_name} &fбыло скрыто AI-Модератором.");
    }

    public String getAiModeratorLoggerHoverMessage() {
        return this.moderation.getString("ai-moderator.logger.hover-message", "&7Причина: &f{reason}");
    }

    public String getAiModeratorPrompt() {
        return this.moderation.getString("ai-moderator.prompt", "");
    }

    public LuckPermsIntegration getLuckPermsIntegration() {
        return this.luckPermsIntegration;
    }

    public PlaceholderAPIIntegration getPlaceholderAPI() {
        return this.placeholderAPI;
    }

    public boolean isDiscordWebhookEnabled() {
        return this.config.getBoolean("discord-webhook.enabled");
    }

    public String getDiscordWebhookUrl() {
        return this.config.getString("discord-webhook.url");
    }

    public String getDiscordWebhookUsername() {
        return this.config.getString("discord-webhook.username", "Minecraft");
    }

    public String getDiscordWebhookAvatarUrl() {
        return this.config.getString("discord-webhook.avatar-url", "");
    }

    public boolean isPlayerInfoEnabled() {
        return this.config.getBoolean("player-info.enabled");
    }

    public String getPlayerInfoPermission() {
        return this.config.getString("player-info.permission");
    }

    public List<String> getPlayerInfoLines() {
        return this.config.getStringList("player-info.lines");
    }

    public FcChat getPlugin() {
        return this.plugin;
    }

    public boolean updateCheck() {
        return this.config.getBoolean("update-check");
    }

    public List<String> getDisabledWorlds() {
        return this.config.getStringList("disabled-worlds");
    }

    public boolean isJoinLeaveMessagesEnabled() {
        return this.config.getBoolean("join-leave-messages.enabled");
    }

    public String getJoinMessage() {
        return this.config.getString("join-leave-messages.join.message");
    }

    public String getJoinSound() {
        return this.config.getString("join-leave-messages.join.sound");
    }

    public String getLeaveMessage() {
        return this.config.getString("join-leave-messages.leave.message");
    }

    public String getLeaveSound() {
        return this.config.getString("join-leave-messages.leave.sound");
    }

    public boolean isHologramMessagesEnabled() {
        return this.cachedHologramEnabled;
    }

    public int getHologramDuration() {
        return this.config.getInt("hologram-messages.duration");
    }

    public int getHologramMaxWordsPerLine() {
        return this.config.getInt("hologram-messages.max-words-per-line");
    }

    public double getHologramHeight() {
        return this.config.getDouble("hologram-messages.height");
    }

    public String getHologramMessageFormat() {
        return this.config.getString("hologram-messages.format");
    }

    public boolean isMessageSoundEnabled() {
        return this.config.getBoolean("sounds.message.enabled");
    }

    public String getMessageSound() {
        return this.config.getString("sounds.message.sound");
    }

    public float getMessageSoundVolume() {
        return (float)this.config.getDouble("sounds.message.volume");
    }

    public float getMessageSoundPitch() {
        return (float)this.config.getDouble("sounds.message.pitch");
    }

    public boolean isPingSystemEnabled() {
        return this.config.getBoolean("ping-system.enabled");
    }

    public String getPingSymbol() {
        return this.config.getString("ping-system.symbol", "@");
    }

    public String getPingPermission() {
        return this.config.getString("ping-system.permission");
    }

    public String getPingColor() {
        return this.config.getString("ping-system.ping-color");
    }

    public boolean isPingSoundEnabled() {
        return this.config.getBoolean("sounds.ping.enabled");
    }

    public String getPingSound() {
        return this.config.getString("sounds.ping.sound");
    }

    public float getPingSoundVolume() {
        return (float)this.config.getDouble("sounds.ping.volume");
    }

    public float getPingSoundPitch() {
        return (float)this.config.getDouble("sounds.ping.pitch");
    }

    public String getEveryonePingPermission() {
        return this.config.getString("ping-system.everyone-permission");
    }

    public String getEveryonePingColor() {
        return this.config.getString("ping-system.everyone-ping-color");
    }

    public boolean isEveryonePingSoundEnabled() {
        return this.config.getBoolean("sounds.everyone-ping.enabled");
    }

    public String getEveryonePingSound() {
        return this.config.getString("sounds.everyone-ping.sound");
    }

    public float getEveryonePingSoundVolume() {
        return (float)this.config.getDouble("sounds.everyone-ping.volume");
    }

    public float getEveryonePingSoundPitch() {
        return (float)this.config.getDouble("sounds.everyone-ping.pitch");
    }

    public boolean isCopyEnabled() {
        return this.config.getBoolean("copy.enabled", true);
    }

    public String getCopyPermission() {
        return this.config.getString("copy.permission", "fcchat.copy");
    }

    public String getCopyHoverText() {
        return this.config.getString("copy.hover-text", "&7[&fClick to copy&7]");
    }

    public boolean isWorldColorsEnabled() {
        return this.config.getBoolean("world-colors.enabled", true);
    }

    public String getWorldColor(String worldName) {
        return this.config.getString("world-colors.worlds." + worldName, getDefaultWorldColor());
    }

    public String getDefaultWorldColor() {
        return this.config.getString("world-colors.default-color", "&f");
    }

    public String getEventPriority() {
        return this.config.getString("event-priority", "NORMAL");
    }

    public FileConfiguration getPrivateMessageConfig() {
        return this.privateMessages;
    }

    public boolean isPingToastEnabled() {
        return this.config.getBoolean("ping-system.toast.enabled", true);
    }

    public String getPingToastText() {
        return this.config.getString("ping-system.toast.text", "&fТебя упомянули в чате!");
    }

    public boolean isCommandEnabled(String command) {
        return this.config.getBoolean("commands." + command + ".enabled", true);
    }

    public boolean isCommandAliasEnabled(String command, String alias) {
        return this.config.getBoolean("commands." + command + ".aliases." + alias, true);
    }
}
