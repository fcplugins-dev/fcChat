package fc.plugins.fcchat.channel;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.chat.MessageProcessor;
import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.data.PlayerTimeManager;
import fc.plugins.fcchat.function.Copy;
import fc.plugins.fcchat.sync.MessageSynchronizer;
import fc.plugins.fcchat.function.Spy;
import fc.plugins.fcchat.integration.LuckPermsIntegration;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import fc.plugins.fcchat.integration.DiscordIntegration;
import fc.plugins.fcchat.moderation.Filter;
import fc.plugins.fcchat.moderation.LinkBlocker;
import fc.plugins.fcchat.moderation.AntiSpam;
import fc.plugins.fcchat.utils.HexUtils;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ChannelManager {
    private final FcChat plugin;
    private final ConfigManager configManager;
    private final PlayerTimeManager playerTimeManager;
    private final Copy copyFunction;
    private final MessageSynchronizer messageSynchronizer;
    private final Spy spyFunction;
    private final Filter filter;
    private final LinkBlocker linkBlocker;
    private final AntiSpam antiSpam;
    private final Map<String, Channel> channels;
    private final Map<UUID, String> playerChannels;
    private File channelFile;
    private FileConfiguration channelConfig;

    public ChannelManager(FcChat plugin, ConfigManager configManager, PlayerTimeManager playerTimeManager, MessageSynchronizer messageSynchronizer) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerTimeManager = playerTimeManager;
        this.messageSynchronizer = messageSynchronizer;
        this.copyFunction = new Copy(configManager);
        this.spyFunction = new Spy(configManager);
        this.filter = new Filter(configManager);
        this.linkBlocker = new LinkBlocker(configManager);
        this.antiSpam = new AntiSpam(configManager, playerTimeManager);
        this.channels = new HashMap<>();
        this.playerChannels = new HashMap<>();
        loadChannels();
    }

    public void loadChannels() {
        channelFile = new File(plugin.getDataFolder(), "channel.yml");
        if (!channelFile.exists()) {
            plugin.saveResource("channel.yml", false);
        }
        channelConfig = YamlConfiguration.loadConfiguration(channelFile);
        channels.clear();

        ConfigurationSection channelsSection = channelConfig.getConfigurationSection("channels");
        if (channelsSection != null) {
            for (String channelId : channelsSection.getKeys(false)) {
                ConfigurationSection channelSection = channelsSection.getConfigurationSection(channelId);
                if (channelSection != null) {
                    Channel channel = Channel.fromConfig(channelSection, channelId);
                    channels.put(channelId, channel);
                }
            }
        }
    }

    public void saveChannels() {
        try {
            channelConfig.save(channelFile);
        } catch (IOException e) {
        }
    }

    public void reloadChannels() {
        loadChannels();
    }

    public Channel getChannel(String id) {
        return channels.get(id);
    }

    public Map<String, Channel> getAllChannels() {
        return channels;
    }

    public String getPlayerChannel(UUID playerId) {
        return playerChannels.getOrDefault(playerId, "default");
    }

    public void setPlayerChannel(UUID playerId, String channelId) {
        if (channelId.equals("default")) {
            playerChannels.remove(playerId);
        } else {
            playerChannels.put(playerId, channelId);
        }
    }

    public boolean hasChannelPermission(Player player, String channelId) {
        if (channelId.equals("default")) {
            return true;
        }
        Channel channel = getChannel(channelId);
        if (channel == null || !channel.isEnabled()) {
            return false;
        }
        return player.hasPermission(channel.getPermission());
    }

    public void handleChannelChat(Player sender, String message) {
        String channelId = getPlayerChannel(sender.getUniqueId());
        Channel channel = getChannel(channelId);

        if (channel == null || !channel.isEnabled()) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("channel.not-found")));
            return;
        }

        if (!sender.hasPermission(channel.getPermission())) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("channel.no-permission")));
            return;
        }

        boolean hasBypass = sender.hasPermission("fcchat.bypass");

        if (linkBlocker.isBlocked(message) && !hasBypass) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', linkBlocker.getBlockedMessage()));
            return;
        }

        if (message.contains("%") && message.indexOf("%") != message.lastIndexOf("%")) {
            return;
        }

        if (message.contains("||") && !sender.hasPermission(configManager.getHiddenTextPermission())) {
            return;
        }

        if (antiSpam.isSpam(sender) && !hasBypass) {
            double remainingTime = antiSpam.getRemainingSpamTime(sender);
            sender.sendMessage(antiSpam.getAntiSpamMessage(remainingTime));
            return;
        }

        if (antiSpam.isNewPlayerBlocked(sender) && !hasBypass) {
            double remainingTime = antiSpam.getRemainingNewPlayerTime(sender);
            sender.sendMessage(antiSpam.getNewPlayerMessage(remainingTime));
            return;
        }

        String filteredMessage = filter.filterMessage(message, sender);
        String formattedMessage = formatChannelMessage(channel, sender, filteredMessage);

        sendChannelMessage(sender, message, filteredMessage, formattedMessage, channel);

        DiscordIntegration discord = configManager.getDiscordIntegration();
        if (discord.isEnabled()) {
            discord.sendMessage(sender, filteredMessage);
        }

        if (configManager.isSpyEnabled()) {
            spyFunction.sendSpyMessage(sender, filteredMessage, formattedMessage);
        }
    }

    private void sendChannelMessage(Player sender, String originalMessage, String filteredMessage, String formattedMessage, Channel channel) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (hasChannelPermission(player, channel.getId())) {
                TextComponent finalComponent = createChannelMessageComponent(formattedMessage, originalMessage, filteredMessage, sender, player);
                player.spigot().sendMessage(finalComponent);
            }
        }
    }

    private TextComponent createChannelMessageComponent(String formattedMessage, String originalMessage, String filteredMessage, Player sender, Player receiver) {
        String processedMessage = MessageProcessor.processHiddenText(filteredMessage, configManager);
        String finalFormattedMessage = formattedMessage;
        
        boolean hasHiddenText = filteredMessage.contains("||") && sender.hasPermission(configManager.getHiddenTextPermission());
        boolean wasFiltered = !originalMessage.equals(filteredMessage);
        boolean canReadBlocked = receiver.hasPermission("fcchat.read");
        boolean canSeePlayerInfo = receiver.hasPermission(configManager.getPlayerInfoPermission()) && configManager.isPlayerInfoEnabled();
        
        if (canSeePlayerInfo) {
            return createPlayerInfoMessage(formattedMessage, originalMessage, filteredMessage, processedMessage, sender, receiver, hasHiddenText, wasFiltered, canReadBlocked);
        }
        
        if (hasHiddenText && finalFormattedMessage.contains(processedMessage)) {
            return MessageProcessor.createHiddenTextComponent(formattedMessage, filteredMessage, configManager);
        }
        
        if (wasFiltered && canReadBlocked && finalFormattedMessage.contains(processedMessage)) {
            String prefix = finalFormattedMessage.substring(0, finalFormattedMessage.lastIndexOf(processedMessage));
            String suffix = finalFormattedMessage.substring(finalFormattedMessage.lastIndexOf(processedMessage) + processedMessage.length());
            
            TextComponent prefixComponent = new TextComponent(prefix);
            TextComponent messageComponent = new TextComponent(processedMessage);
            TextComponent suffixComponent = new TextComponent(suffix);
            
            net.md_5.bungee.api.chat.HoverEvent filterHover = new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("§f" + originalMessage).create()
            );
            messageComponent.setHoverEvent(filterHover);
            
            if (receiver.hasPermission(configManager.getCopyPermission()) && configManager.isCopyEnabled()) {
                messageComponent = copyFunction.addClickEvent(messageComponent, originalMessage);
            }
            
            prefixComponent.addExtra(messageComponent);
            prefixComponent.addExtra(suffixComponent);
            return prefixComponent;
        }
        
        TextComponent component;
        
        if (receiver.hasPermission(configManager.getCopyPermission()) && configManager.isCopyEnabled()) {
            if (hasHiddenText) {
                component = MessageProcessor.createHiddenTextComponent(formattedMessage, filteredMessage, configManager);
            } else {
                component = copyFunction.createClickableMessage(formattedMessage, originalMessage);
            }
        } else {
            if (hasHiddenText) {
                component = MessageProcessor.createHiddenTextComponent(formattedMessage, filteredMessage, configManager);
            } else {
                component = new TextComponent(formattedMessage);
            }
        }
        
        return component;
    }
    
    private TextComponent createPlayerInfoMessage(String formattedMessage, String originalMessage, String filteredMessage, String processedMessage, Player sender, Player receiver, boolean hasHiddenText, boolean wasFiltered, boolean canReadBlocked) {
        String playerName = sender.getName();
        
        if (!formattedMessage.contains(playerName)) {
            return new TextComponent(formattedMessage);
        }
        
        String beforePlayer = formattedMessage.substring(0, formattedMessage.indexOf(playerName));
        String afterPlayer = formattedMessage.substring(formattedMessage.indexOf(playerName) + playerName.length());
        
        TextComponent beforeComponent = new TextComponent(beforePlayer);
        TextComponent playerComponent = new TextComponent(playerName);
        
        String playerInfoText = createPlayerInfoText(sender);
        if (playerInfoText != null && !playerInfoText.isEmpty()) {
            net.md_5.bungee.api.chat.HoverEvent playerInfoHover = new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder(playerInfoText).create()
            );
            playerComponent.setHoverEvent(playerInfoHover);
        }
        
        if (hasHiddenText && afterPlayer.contains(processedMessage)) {
            TextComponent hiddenComponent = MessageProcessor.createHiddenTextComponent(afterPlayer, filteredMessage, configManager);
            
            if (receiver.hasPermission(configManager.getCopyPermission()) && configManager.isCopyEnabled()) {
                hiddenComponent = copyFunction.addClickEvent(hiddenComponent, originalMessage);
            }
            
            beforeComponent.addExtra(playerComponent);
            beforeComponent.addExtra(hiddenComponent);
            return beforeComponent;
        } else if (wasFiltered && canReadBlocked && afterPlayer.contains(processedMessage)) {
            String beforeMessage = afterPlayer.substring(0, afterPlayer.indexOf(processedMessage));
            String afterMessage = afterPlayer.substring(afterPlayer.indexOf(processedMessage) + processedMessage.length());
            
            TextComponent beforeMsgComponent = new TextComponent(beforeMessage);
            TextComponent messageComponent = new TextComponent(processedMessage);
            TextComponent afterMsgComponent = new TextComponent(afterMessage);
            
            net.md_5.bungee.api.chat.HoverEvent filterHover = new net.md_5.bungee.api.chat.HoverEvent(
                net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
                new net.md_5.bungee.api.chat.ComponentBuilder("§f" + originalMessage).create()
            );
            messageComponent.setHoverEvent(filterHover);
            
            if (receiver.hasPermission(configManager.getCopyPermission()) && configManager.isCopyEnabled()) {
                beforeMsgComponent = copyFunction.addClickEvent(beforeMsgComponent, originalMessage);
                messageComponent = copyFunction.addClickEvent(messageComponent, originalMessage);
                afterMsgComponent = copyFunction.addClickEvent(afterMsgComponent, originalMessage);
            }
            
            beforeComponent.addExtra(playerComponent);
            beforeComponent.addExtra(beforeMsgComponent);
            beforeComponent.addExtra(messageComponent);
            beforeComponent.addExtra(afterMsgComponent);
            return beforeComponent;
        } else {
            TextComponent afterComponent = new TextComponent(afterPlayer);
            
            if (receiver.hasPermission(configManager.getCopyPermission()) && configManager.isCopyEnabled()) {
                afterComponent = copyFunction.addClickEvent(afterComponent, originalMessage);
            }
            
            beforeComponent.addExtra(playerComponent);
            beforeComponent.addExtra(afterComponent);
            return beforeComponent;
        }
    }
    
    private String createPlayerInfoText(Player player) {
        java.util.List<String> infoLines = configManager.getPlayerInfoLines();
        if (infoLines.isEmpty()) {
            return null;
        }
        
        StringBuilder hoverText = new StringBuilder();
        
        for (String line : infoLines) {
            if (hoverText.length() > 0) {
                hoverText.append("\n");
            }
            
            String processedLine = line;
            
            fc.plugins.fcchat.integration.PlaceholderAPIIntegration placeholderAPI = configManager.getPlaceholderAPI();
            if (placeholderAPI != null && placeholderAPI.isEnabled()) {
                try {
                    processedLine = placeholderAPI.setPlaceholders(player, processedLine);
                } catch (Exception e) {
                }
            }
            
            hoverText.append(processedLine);
        }
        
        return fc.plugins.fcchat.utils.HexUtils.translateAlternateColorCodes(hoverText.toString());
    }

    private String formatChannelMessage(Channel channel, Player player, String message) {
        String formattedMessage = channel.getFormat()
                .replace("{player}", player.getName())
                .replace("{message}", processMessageColors(message, player))
                .replace("{channel}", channel.getName());

        LuckPermsIntegration luckPerms = configManager.getLuckPermsIntegration();
        if (luckPerms.isEnabled()) {
            String prefix = HexUtils.translateAlternateColorCodes(luckPerms.getPrefix(player, ""));
            String suffix = HexUtils.translateAlternateColorCodes(luckPerms.getSuffix(player, ""));
            formattedMessage = formattedMessage
                    .replace("%prefix%", prefix)
                    .replace("%suffix%", suffix);
        } else {
            formattedMessage = formattedMessage
                    .replace("%prefix%", "")
                    .replace("%suffix%", "");
        }

        PlaceholderAPIIntegration placeholderAPI = configManager.getPlaceholderAPI();
        if (placeholderAPI.isEnabled()) {
            formattedMessage = placeholderAPI.setPlaceholders(player, formattedMessage);
        }

        return HexUtils.translateAlternateColorCodes(formattedMessage);
    }

    private String processMessageColors(String message, Player player) {
        if (!configManager.isColorChatEnabled()) {
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));
        }

        if (!player.hasPermission(configManager.getColorChatPermission())) {
            return ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', message));
        }

        return ChatColor.translateAlternateColorCodes('&', message);
    }
} 