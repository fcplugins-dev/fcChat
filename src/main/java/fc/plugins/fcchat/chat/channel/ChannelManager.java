
package fc.plugins.fcchat.chat.channel;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.chat.MessageProcessor;
import fc.plugins.fcchat.chat.PingManager;
import fc.plugins.fcchat.chat.SoundManager;
import fc.plugins.fcchat.chat.channel.Channel;
import fc.plugins.fcchat.integration.LuckPermsIntegration;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.moderation.AiModerator;
import fc.plugins.fcchat.moderation.AntiSpam;
import fc.plugins.fcchat.moderation.Filter;
import fc.plugins.fcchat.moderation.LinkBlocker;
import fc.plugins.fcchat.utils.HexUtils;
import fc.plugins.fcchat.utils.data.PlayerTimeManager;
import fc.plugins.fcchat.utils.function.Copy;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class ChannelManager {
    private final FcChat plugin;
    private final ConfigManager configManager;
    private final Copy copyFunction;
    private final Filter filter;
    private final LinkBlocker linkBlocker;
    private final AntiSpam antiSpam;
    private final AiModerator aiModerator;
    private final Map<String, Channel> channels;
    private final Map<UUID, String> playerChannels;
    private File channelFile;
    private FileConfiguration channelConfig;
    private final SoundManager soundManager;
    private final PingManager pingManager;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('\u00A7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public ChannelManager(FcChat plugin, ConfigManager configManager, PlayerTimeManager playerTimeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.copyFunction = new Copy(configManager);
        this.filter = new Filter(configManager);
        this.linkBlocker = new LinkBlocker(configManager);
        this.antiSpam = new AntiSpam(configManager, playerTimeManager);
        this.aiModerator = plugin.getAiModerator();
        this.channels = new HashMap<String, Channel>();
        this.playerChannels = new HashMap<UUID, String>();
        this.soundManager = new SoundManager(configManager);
        this.pingManager = new PingManager(configManager, this.soundManager);
        this.loadChannels();
    }

    public void loadChannels() {
        this.channelFile = new File(this.plugin.getDataFolder(), "channel.yml");
        if (!this.channelFile.exists()) {
            this.plugin.saveResource("channel.yml", false);
        }
        this.channelConfig = YamlConfiguration.loadConfiguration(this.channelFile);
        this.channels.clear();
        ConfigurationSection channelsSection = this.channelConfig.getConfigurationSection("channels");
        if (channelsSection != null) {
            for (String channelId : channelsSection.getKeys(false)) {
                ConfigurationSection channelSection = channelsSection.getConfigurationSection(channelId);
                if (channelSection == null) continue;
                Channel channel = Channel.fromConfig(channelSection, channelId);
                this.channels.put(channelId, channel);
            }
        }
    }

    public void saveChannels() {
        try {
            this.channelConfig.save(this.channelFile);
        }
        catch (IOException iOException) {
            // empty catch block
        }
    }

    public void reloadChannels() {
        this.loadChannels();
    }

    public Channel getChannel(String id) {
        return this.channels.get(id);
    }

    public Map<String, Channel> getAllChannels() {
        return this.channels;
    }

    public String getPlayerChannel(UUID playerId) {
        return this.playerChannels.getOrDefault(playerId, "default");
    }

    public void setPlayerChannel(UUID playerId, String channelId) {
        if (channelId.equals("default")) {
            this.playerChannels.remove(playerId);
        } else {
            this.playerChannels.put(playerId, channelId);
        }
    }

    public boolean hasChannelPermission(Player player, String channelId) {
        if (channelId.equals("default")) {
            return true;
        }
        Channel channel = this.getChannel(channelId);
        if (channel == null || !channel.isEnabled()) {
            return false;
        }
        if (channel.isClanChannel()) {
            return this.hasClanAccess(player, channel);
        }
        return player.hasPermission(channel.getPermission());
    }

    private boolean hasClanAccess(Player player, Channel channel) {
        PlaceholderAPIIntegration placeholderAPI = this.configManager.getPlaceholderAPI();
        if (placeholderAPI == null || !placeholderAPI.isEnabled()) {
            return false;
        }
        try {
            String clanName;
            try {
                clanName = placeholderAPI.setPlaceholders(player, channel.getPlaceholder());
            }
            catch (Exception e) {
                clanName = channel.getPlaceholderNoClan();
            }
            return !clanName.equals(channel.getPlaceholderNoClan());
        }
        catch (Exception e) {
            return false;
        }
    }

    public String getPlayerClanName(Player player, Channel channel) {
        if (!channel.isClanChannel()) {
            return null;
        }
        PlaceholderAPIIntegration placeholderAPI = this.configManager.getPlaceholderAPI();
        if (placeholderAPI == null || !placeholderAPI.isEnabled()) {
            return channel.getPlaceholderNoClan();
        }
        try {
            return placeholderAPI.setPlaceholders(player, channel.getPlaceholder());
        }
        catch (Exception e) {
            try {
                return channel.getPlaceholderNoClan();
            }
            catch (Exception e2) {
                return channel.getPlaceholderNoClan();
            }
        }
    }

    public void handleChannelChat(Player sender, String message) {
        String channelId = this.getPlayerChannel(sender.getUniqueId());
        Channel channel = this.getChannel(channelId);
        if (channel == null || !channel.isEnabled()) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.not-found")));
            return;
        }
        if (channel.isClanChannel()) {
            String clanName = this.getPlayerClanName(sender, channel);
            if (clanName.equals(channel.getPlaceholderNoClan())) {
                sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.no-clan")));
                return;
            }
        } else if (!sender.hasPermission(channel.getPermission())) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.no-permission")));
            return;
        }
        boolean hasBypass = sender.hasPermission("fcchat.bypass");
        if (this.linkBlocker.isBlocked(message) && !hasBypass) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.linkBlocker.getBlockedMessage()));
            return;
        }
        if (message.contains("%") && message.indexOf("%") != message.lastIndexOf("%")) {
            return;
        }
        if (message.contains("||") && !sender.hasPermission(this.configManager.getHiddenTextPermission())) {
            return;
        }
        if (this.antiSpam.isSpam(sender) && !hasBypass) {
            double remainingTime = this.antiSpam.getRemainingSpamTime(sender);
            sender.sendMessage(this.antiSpam.getAntiSpamMessage(remainingTime));
            return;
        }
        if (this.antiSpam.isNewPlayerBlocked(sender) && !hasBypass) {
            double remainingTime = this.antiSpam.getRemainingNewPlayerTime(sender);
            sender.sendMessage(this.antiSpam.getNewPlayerMessage(remainingTime));
            return;
        }
        String filteredMessage = this.filter.filterMessage(message, sender);
        PingManager.PingResult pingResult = this.pingManager.processPings(filteredMessage, sender);
        String formattedMessage = this.formatChannelMessage(channel, sender, pingResult.getProcessedMessage());
        boolean aiActive = this.aiModerator.isActiveFor(sender);
        if (aiActive) {
            Component senderPreview = this.createChannelMessageComponent(formattedMessage, message, pingResult.getProcessedMessage(), sender, sender);
            sender.sendMessage(senderPreview);
            AiModerator.Decision decision = this.aiModerator.moderate(sender, message);
            if (decision.isBlocked()) {
                this.aiModerator.registerBlocked(sender, decision.getReason());
                sender.sendMessage(this.aiModerator.getBlockedMessage());
                return;
            }
        }
        this.sendChannelMessage(sender, message, pingResult.getProcessedMessage(), formattedMessage, channel, aiActive);
        this.soundManager.playMessageSound(sender);
        this.pingManager.playPingSounds(pingResult.getPingedPlayers(), pingResult.hasEveryonePing());
    }

    public Channel getChannelBySymbol(String symbol) {
        for (Channel channel : this.channels.values()) {
            if (!channel.hasSymbol() || !channel.getSymbol().equals(symbol)) continue;
            return channel;
        }
        return null;
    }

    public void handleSymbolChat(Player sender, String message) {
        if (message.length() < 2) {
            return;
        }
        String symbol = message.substring(0, 1);
        String channelMessage = message.substring(1);
        if (channelMessage.trim().isEmpty()) {
            return;
        }
        Channel channel = this.getChannelBySymbol(symbol);
        if (channel == null || !channel.isEnabled()) {
            return;
        }
        if (channel.isClanChannel()) {
            String clanName = this.getPlayerClanName(sender, channel);
            if (clanName.equals(channel.getPlaceholderNoClan())) {
                sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.no-clan")));
                return;
            }
        } else if (!sender.hasPermission(channel.getPermission())) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.no-permission")));
            return;
        }
        boolean hasBypass = sender.hasPermission("fcchat.bypass");
        if (this.linkBlocker.isBlocked(channelMessage) && !hasBypass) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.linkBlocker.getBlockedMessage()));
            return;
        }
        if (channelMessage.contains("%") && channelMessage.indexOf("%") != channelMessage.lastIndexOf("%")) {
            return;
        }
        if (channelMessage.contains("||") && !sender.hasPermission(this.configManager.getHiddenTextPermission())) {
            return;
        }
        if (this.antiSpam.isSpam(sender) && !hasBypass) {
            double remainingTime = this.antiSpam.getRemainingSpamTime(sender);
            sender.sendMessage(this.antiSpam.getAntiSpamMessage(remainingTime));
            return;
        }
        if (this.antiSpam.isNewPlayerBlocked(sender) && !hasBypass) {
            double remainingTime = this.antiSpam.getRemainingNewPlayerTime(sender);
            sender.sendMessage(this.antiSpam.getNewPlayerMessage(remainingTime));
            return;
        }
        String filteredMessage = this.filter.filterMessage(channelMessage, sender);
        PingManager.PingResult pingResult = this.pingManager.processPings(filteredMessage, sender);
        String formattedMessage = this.formatChannelMessage(channel, sender, pingResult.getProcessedMessage());
        boolean aiActive = this.aiModerator.isActiveFor(sender);
        if (aiActive) {
            Component senderPreview = this.createChannelMessageComponent(formattedMessage, channelMessage, pingResult.getProcessedMessage(), sender, sender);
            sender.sendMessage(senderPreview);
            AiModerator.Decision decision = this.aiModerator.moderate(sender, channelMessage);
            if (decision.isBlocked()) {
                this.aiModerator.registerBlocked(sender, decision.getReason());
                sender.sendMessage(this.aiModerator.getBlockedMessage());
                return;
            }
        }
        this.sendChannelMessage(sender, channelMessage, pingResult.getProcessedMessage(), formattedMessage, channel, aiActive);
        this.soundManager.playMessageSound(sender);
        this.pingManager.playPingSounds(pingResult.getPingedPlayers(), pingResult.hasEveryonePing());
    }

    private void sendChannelMessage(Player sender, String originalMessage, String filteredMessage, String formattedMessage, Channel channel, boolean excludeSender) {
        if (channel.isClanChannel()) {
            this.sendClanChannelMessage(sender, originalMessage, filteredMessage, formattedMessage, channel, excludeSender);
        } else {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (excludeSender && player.equals(sender) || !this.hasChannelPermission(player, channel.getId())) continue;
                Component finalComponent = this.createChannelMessageComponent(formattedMessage, originalMessage, filteredMessage, sender, player);
                player.sendMessage(finalComponent);
            }
        }
    }

    private void sendClanChannelMessage(Player sender, String originalMessage, String filteredMessage, String formattedMessage, Channel channel, boolean excludeSender) {
        String senderClanName = this.getPlayerClanName(sender, channel);
        if (senderClanName.equals(channel.getPlaceholderNoClan())) {
            return;
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerClanName;
            if (excludeSender && player.equals(sender) || !(playerClanName = this.getPlayerClanName(player, channel)).equals(senderClanName) || playerClanName.equals(channel.getPlaceholderNoClan())) continue;
            Component finalComponent = this.createChannelMessageComponent(formattedMessage, originalMessage, filteredMessage, sender, player);
            player.sendMessage(finalComponent);
        }
    }

    private Component createChannelMessageComponent(String formattedMessage, String originalMessage, String filteredMessage, Player sender, Player receiver) {
        String processedMessage = MessageProcessor.processHiddenText(filteredMessage, this.configManager);
        int messageIndex = formattedMessage.lastIndexOf(processedMessage);
        if (messageIndex < 0) {
            Component fallback = LEGACY.deserialize(formattedMessage);
            if (this.configManager.isCopyEnabled() && receiver.hasPermission(this.configManager.getCopyPermission())) {
                fallback = this.copyFunction.addClickEvent(fallback, originalMessage);
            }
            return fallback;
        }

        String prefix = formattedMessage.substring(0, messageIndex);
        String messagePart = formattedMessage.substring(messageIndex, messageIndex + processedMessage.length());
        String suffix = formattedMessage.substring(messageIndex + processedMessage.length());

        Component prefixComponent = LEGACY.deserialize(prefix);
        Component messageComponent = LEGACY.deserialize(messagePart);
        Component suffixComponent = LEGACY.deserialize(suffix);

        if (this.configManager.isPlayerInfoEnabled() && receiver.hasPermission(this.configManager.getPlayerInfoPermission())) {
            String playerInfoText = this.createPlayerInfoText(sender);
            if (playerInfoText != null && !playerInfoText.isEmpty()) {
                prefixComponent = this.applyHoverToPlayerName(prefix, sender.getName(), HoverEvent.showText(LEGACY.deserialize(playerInfoText)));
            }
        }

        boolean wasFiltered = !originalMessage.equals(filteredMessage);
        boolean hasHiddenText = filteredMessage.contains("||") && sender.hasPermission(this.configManager.getHiddenTextPermission());
        boolean canReadBlocked = receiver.hasPermission("fcchat.read");

        if (wasFiltered && canReadBlocked) {
            messageComponent = messageComponent.hoverEvent(HoverEvent.showText(LEGACY.deserialize("\u00a7f" + originalMessage)));
        } else if (hasHiddenText) {
            HoverEvent<Component> hiddenTextHover = MessageProcessor.createHiddenTextHover(filteredMessage, this.configManager);
            if (hiddenTextHover != null) {
                messageComponent = messageComponent.hoverEvent(hiddenTextHover);
            }
        }

        if (this.configManager.isCopyEnabled() && receiver.hasPermission(this.configManager.getCopyPermission())) {
            messageComponent = this.copyFunction.addClickEvent(messageComponent, originalMessage);
        }

        return Component.empty().append(prefixComponent).append(messageComponent).append(suffixComponent);
    }

    private Component applyHoverToPlayerName(String prefix, String playerName, HoverEvent<Component> hoverEvent) {
        int index = prefix.lastIndexOf(playerName);
        if (index < 0) {
            return LEGACY.deserialize(prefix);
        }
        String before = prefix.substring(0, index);
        String name = prefix.substring(index, index + playerName.length());
        String after = prefix.substring(index + playerName.length());
        return Component.empty()
            .append(LEGACY.deserialize(before))
            .append(LEGACY.deserialize(name).hoverEvent(hoverEvent))
            .append(LEGACY.deserialize(after));
    }
    private String createPlayerInfoText(Player player) {
        List<String> infoLines = this.configManager.getPlayerInfoLines();
        if (infoLines.isEmpty()) {
            return null;
        }
        StringBuilder hoverText = new StringBuilder();
        for (String line : infoLines) {
            if (hoverText.length() > 0) {
                hoverText.append("\n");
            }
            String processedLine = line;
            PlaceholderAPIIntegration placeholderAPI = this.configManager.getPlaceholderAPI();
            if (placeholderAPI != null && placeholderAPI.isEnabled()) {
                try {
                    processedLine = placeholderAPI.setPlaceholders(player, processedLine);
                }
                catch (Exception exception) {
                    // empty catch block
                }
            }
            hoverText.append(processedLine);
        }
        return HexUtils.translateAlternateColorCodes(hoverText.toString());
    }

    private String formatChannelMessage(Channel channel, Player player, String message) {
        String processedMessage = this.processMessageColors(message, player);
        String formattedMessage = channel.getFormat().replace("{message}", processedMessage).replace("{channel}", channel.getName());
        String playerName = player.getName();
        Object coloredPlayerName = playerName;
        if (this.configManager.isWorldColorsEnabled()) {
            String worldName = player.getWorld().getName();
            String worldColor = this.configManager.getWorldColor(worldName);
            coloredPlayerName = worldColor + playerName + "&r";
        }
        formattedMessage = formattedMessage.replace("{player}", (CharSequence)coloredPlayerName);
        LuckPermsIntegration luckPerms = this.configManager.getLuckPermsIntegration();
        if (luckPerms.isEnabled()) {
            String prefix = luckPerms.getPrefix(player, "");
            String suffix = luckPerms.getSuffix(player, "");
            formattedMessage = formattedMessage.replace("%prefix%", prefix).replace("%suffix%", suffix).replace("%luckperms_prefix%", prefix).replace("%luckperms_suffix%", suffix);
        } else {
            formattedMessage = formattedMessage.replace("%prefix%", "").replace("%suffix%", "").replace("%luckperms_prefix%", "").replace("%luckperms_suffix%", "");
        }
        PlaceholderAPIIntegration placeholderAPI = this.configManager.getPlaceholderAPI();
        if (placeholderAPI != null && placeholderAPI.isEnabled()) {
            try {
                formattedMessage = placeholderAPI.setPlaceholders(player, formattedMessage);
            }
            catch (Exception exception) {
                // empty catch block
            }
        }
        if (this.plugin.getApi() != null) {
            formattedMessage = this.plugin.getApi().applyContextPlaceholders(player, null, formattedMessage, message);
        }
        formattedMessage = HexUtils.translateAlternateColorCodes(formattedMessage);
        return formattedMessage;
    }

    private String processMessageColors(String message, Player player) {
        if (!this.configManager.isColorChatEnabled()) {
            return ChatColor.stripColor(HexUtils.translateAlternateColorCodes(message));
        }
        if (!player.hasPermission(this.configManager.getColorChatPermission())) {
            return ChatColor.stripColor(HexUtils.translateAlternateColorCodes(message));
        }
        return HexUtils.translateAlternateColorCodes(message);
    }

}
