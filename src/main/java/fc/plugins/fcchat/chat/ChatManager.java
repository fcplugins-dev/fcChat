package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.chat.channel.ChannelManager;
import fc.plugins.fcchat.integration.LuckPermsIntegration;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.manager.holograms.HologramsManager;
import fc.plugins.fcchat.moderation.AiModerator;
import fc.plugins.fcchat.moderation.AntiSpam;
import fc.plugins.fcchat.moderation.Filter;
import fc.plugins.fcchat.moderation.LinkBlocker;
import fc.plugins.fcchat.utils.HexUtils;
import fc.plugins.fcchat.utils.data.PlayerTimeManager;
import fc.plugins.fcchat.utils.function.Copy;
import fc.plugins.fcchat.utils.sync.MessageSynchronizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class ChatManager implements Listener {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('\u00A7').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    private final FcChat plugin;
    private final ConfigManager configManager;
    private final PlayerTimeManager playerTimeManager;
    private final ChannelManager channelManager;
    private final Copy copyFunction;
    private final MessageSynchronizer messageSynchronizer;
    private final Filter filter;
    private final LinkBlocker linkBlocker;
    private final AntiSpam antiSpam;
    private final AiModerator aiModerator;
    private final HologramsManager hologramsManager;
    private final SoundManager soundManager;
    private final PingManager pingManager;

    public ChatManager(FcChat plugin, ConfigManager configManager, PlayerTimeManager playerTimeManager, MessageSynchronizer messageSynchronizer, HologramsManager hologramsManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerTimeManager = playerTimeManager;
        this.messageSynchronizer = messageSynchronizer;
        this.hologramsManager = hologramsManager;
        this.channelManager = new ChannelManager(plugin, configManager, playerTimeManager);
        this.copyFunction = new Copy(configManager);
        this.filter = new Filter(configManager);
        this.linkBlocker = new LinkBlocker(configManager);
        this.antiSpam = new AntiSpam(configManager, playerTimeManager);
        this.aiModerator = plugin.getAiModerator();
        this.soundManager = new SoundManager(configManager);
        this.pingManager = new PingManager(configManager, this.soundManager);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled()) {
            return;
        }
        if (!this.plugin.getConfig().getBoolean("chat.enabled", true)) {
            return;
        }
        Player player = event.getPlayer();
        String message = event.getMessage();
        String prefix = this.configManager.getChatPrefix();
        if (this.configManager.getDisabledWorlds().contains(player.getWorld().getName())) {
            event.setCancelled(true);
            return;
        }
        boolean hasBypass = player.hasPermission("fcchat.bypass");
        if (this.linkBlocker.isBlocked(message, player)) {
            event.setCancelled(true);
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.linkBlocker.getBlockedMessage()));
            return;
        }
        if (message.contains("%") && message.indexOf("%") != message.lastIndexOf("%")) {
            event.setCancelled(true);
            return;
        }
        if (message.contains("||") && !player.hasPermission(this.configManager.getHiddenTextPermission())) {
            event.setCancelled(true);
            return;
        }
        if (this.antiSpam.isSpam(player) && !hasBypass) {
            event.setCancelled(true);
            double remainingTime = this.antiSpam.getRemainingSpamTime(player);
            player.sendMessage(this.antiSpam.getAntiSpamMessage(remainingTime));
            return;
        }
        if (this.antiSpam.isNewPlayerBlocked(player) && !hasBypass) {
            event.setCancelled(true);
            double remainingTime = this.antiSpam.getRemainingNewPlayerTime(player);
            player.sendMessage(this.antiSpam.getNewPlayerMessage(remainingTime));
            return;
        }
        if (this.antiSpam.hasTooManyCaps(message, player) && !hasBypass) {
            String mode = this.configManager.getAntiCapsMode();
            if ("block".equalsIgnoreCase(mode)) {
                event.setCancelled(true);
                player.sendMessage(this.antiSpam.getAntiCapsMessage());
                return;
            }
            message = this.antiSpam.processCaps(message, player);
        }
        event.setCancelled(true);
        if (this.channelManager.getChannelBySymbol(message.substring(0, 1)) != null) {
            this.channelManager.handleSymbolChat(player, message);
            return;
        }
        String currentChannel = this.channelManager.getPlayerChannel(player.getUniqueId());
        if (!currentChannel.equals("default")) {
            this.channelManager.handleChannelChat(player, message);
            return;
        }
        if (message.startsWith(prefix)) {
            String chatMessage = message.substring(prefix.length());
            if (chatMessage.trim().isEmpty()) {
                return;
            }
            if (chatMessage.startsWith(" ")) {
                chatMessage = chatMessage.substring(1);
            }
            this.handleGlobalChat(player, chatMessage);
            String formattedConsoleMessage = this.formatMessage(this.configManager.getGlobalChatFormat(), player, chatMessage);
            this.plugin.getLogger().info(ChatColor.stripColor(formattedConsoleMessage));
        } else {
            this.handleLocalChat(player, message);
            String formattedConsoleMessage = this.formatMessage(this.configManager.getLocalChatFormat(), player, message);
            this.plugin.getLogger().info(ChatColor.stripColor(formattedConsoleMessage));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (this.hologramsManager != null) {
            this.hologramsManager.onPlayerQuit(event.getPlayer());
        }
    }

    public void reloadModeration() {
        this.filter.reloadFilter();
        this.linkBlocker.reloadModeration();
    }

    public ChannelManager getChannelManager() {
        return this.channelManager;
    }

    public PlayerTimeManager getPlayerTimeManager() {
        return this.playerTimeManager;
    }

    public Copy getCopyFunction() {
        return this.copyFunction;
    }

    private void handleLocalChat(Player sender, String message) {
        String originalMessage = message;
        String filteredMessage = this.filter.filterMessage(message, sender);
        boolean wasFiltered = this.filter.wasMessageFiltered(originalMessage, filteredMessage);
        PingManager.PingResult pingResult = this.pingManager.processPings(filteredMessage, sender);
        String processedMessage = MessageProcessor.processHiddenText(pingResult.getProcessedMessage(), this.configManager);
        int radius = this.configManager.getLocalChatRadius();
        Location senderLocation = sender.getLocation();
        int radiusSquared = radius * radius;
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        ArrayList<Player> recipients = new ArrayList<>(onlinePlayers.size());
        for (Player player : onlinePlayers) {
            if (!player.getWorld().equals(sender.getWorld()) || player.getLocation().distanceSquared(senderLocation) > (double) radiusSquared) {
                continue;
            }
            recipients.add(player);
        }
        HoverEvent<Component> hiddenTextHover = MessageProcessor.createHiddenTextHover(pingResult.getProcessedMessage(), this.configManager);
        boolean aiActive = this.aiModerator.isActiveFor(sender);
        if (aiActive) {
            Component senderPreview = this.buildMessageComponent(sender, sender, processedMessage, originalMessage, wasFiltered, hiddenTextHover, this.configManager.getLocalChatFormat());
            sender.sendMessage(senderPreview);
            AiModerator.Decision decision = this.aiModerator.moderate(sender, originalMessage);
            if (decision.isBlocked()) {
                this.aiModerator.registerBlocked(sender, decision.getReason());
                sender.sendMessage(this.aiModerator.getBlockedMessage());
                return;
            }
        }
        for (Player player : recipients) {
            if (aiActive && player.equals(sender)) {
                continue;
            }
            Component finalComponent = this.buildMessageComponent(sender, player, processedMessage, originalMessage, wasFiltered, hiddenTextHover, this.configManager.getLocalChatFormat());
            player.sendMessage(finalComponent);
        }
        this.soundManager.playMessageSound(sender);
        this.pingManager.playPingSounds(pingResult.getPingedPlayers(), pingResult.hasEveryonePing());
        if (this.hologramsManager != null && this.configManager.isHologramMessagesEnabled()) {
            String hologramMessage = pingResult.getProcessedMessage();
            this.plugin.getCompatScheduler().runEntity(sender, () -> this.hologramsManager.createHologram(sender, hologramMessage));
        }
    }

    private void handleGlobalChat(Player sender, String message) {
        String originalMessage = message;
        String filteredMessage = this.filter.filterMessage(message, sender);
        boolean wasFiltered = this.filter.wasMessageFiltered(originalMessage, filteredMessage);
        PingManager.PingResult pingResult = this.pingManager.processPings(filteredMessage, sender);
        String processedMessage = MessageProcessor.processHiddenText(pingResult.getProcessedMessage(), this.configManager);
        Collection<? extends Player> onlinePlayers = Bukkit.getOnlinePlayers();
        HoverEvent<Component> hiddenTextHover = MessageProcessor.createHiddenTextHover(pingResult.getProcessedMessage(), this.configManager);
        boolean aiActive = this.aiModerator.isActiveFor(sender);
        if (aiActive) {
            Component senderPreview = this.buildMessageComponent(sender, sender, processedMessage, originalMessage, wasFiltered, hiddenTextHover, this.configManager.getGlobalChatFormat());
            sender.sendMessage(senderPreview);
            AiModerator.Decision decision = this.aiModerator.moderate(sender, originalMessage);
            if (decision.isBlocked()) {
                this.aiModerator.registerBlocked(sender, decision.getReason());
                sender.sendMessage(this.aiModerator.getBlockedMessage());
                return;
            }
        }
        for (Player player : onlinePlayers) {
            if (aiActive && player.equals(sender)) {
                continue;
            }
            Component finalComponent = this.buildMessageComponent(sender, player, processedMessage, originalMessage, wasFiltered, hiddenTextHover, this.configManager.getGlobalChatFormat());
            player.sendMessage(finalComponent);
        }
        this.soundManager.playMessageSound(sender);
        this.pingManager.playPingSounds(pingResult.getPingedPlayers(), pingResult.hasEveryonePing());
        this.messageSynchronizer.syncGlobalMessage(sender, pingResult.getProcessedMessage());
        String strippedMessage = ChatColor.stripColor(processedMessage);
        this.plugin.getCompatScheduler().runAsync(() -> this.plugin.getDiscordSrvIntegration().sendGlobalChat(sender, strippedMessage));
        if (this.hologramsManager != null && this.configManager.isHologramMessagesEnabled()) {
            String hologramMessage = pingResult.getProcessedMessage();
            this.plugin.getCompatScheduler().runEntity(sender, () -> this.hologramsManager.createHologram(sender, hologramMessage));
        }
    }

    private Component buildMessageComponent(Player sender, Player receiver, String processedMessage, String originalMessage, boolean wasFiltered, HoverEvent<Component> hiddenTextHover, String format) {
        String formattedMessage = this.formatMessage(format, sender, processedMessage);
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

        if (wasFiltered) {
            String filterHoverText = format.replace("{message}", originalMessage);
            filterHoverText = this.formatMessage(filterHoverText, sender, originalMessage);
            messageComponent = messageComponent.hoverEvent(HoverEvent.showText(LEGACY.deserialize(filterHoverText)));
        } else if (hiddenTextHover != null) {
            messageComponent = messageComponent.hoverEvent(hiddenTextHover);
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
                } catch (Exception | NoSuchFieldError | NoSuchMethodError ignored) {
                }
            }
            hoverText.append(processedLine);
        }
        return HexUtils.translateAlternateColorCodes(hoverText.toString());
    }

    private String formatMessage(String format, Player player, String message) {
        String processedMessage = this.processMessageColors(message, player);
        String formattedMessage = format.replace("{message}", processedMessage);
        String playerName = player.getName();
        String coloredPlayerName = playerName;
        if (this.configManager.isWorldColorsEnabled()) {
            String worldName = player.getWorld().getName();
            String worldColor = this.configManager.getWorldColor(worldName);
            coloredPlayerName = worldColor + playerName + "&r";
        }
        formattedMessage = formattedMessage.replace("{player}", coloredPlayerName);
        formattedMessage = formattedMessage.replace("{player_name}", coloredPlayerName);
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
            } catch (Exception | NoSuchFieldError | NoSuchMethodError ignored) {
            }
        }
        if (this.plugin.getApi() != null) {
            formattedMessage = this.plugin.getApi().applyContextPlaceholders(player, null, formattedMessage, message);
        }
        return HexUtils.translateAlternateColorCodes(formattedMessage);
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
