package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.channel.ChannelManager;
import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.data.PlayerTimeManager;
import fc.plugins.fcchat.function.Copy;
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
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.ChatColor;

public class ChatManager implements Listener {
    private final FcChat plugin;
    private final ConfigManager configManager;
    private final PlayerTimeManager playerTimeManager;
    private final ChannelManager channelManager;
    private final Copy copyFunction;
    private final Spy spyFunction;
    private final Filter filter;
    private final LinkBlocker linkBlocker;
    private final AntiSpam antiSpam;
    private final PlayerInfoManager playerInfoManager;

    public ChatManager(FcChat plugin, ConfigManager configManager, PlayerTimeManager playerTimeManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.playerTimeManager = playerTimeManager;
        this.channelManager = new ChannelManager(plugin, configManager, playerTimeManager);
        this.copyFunction = new Copy(configManager);
        this.spyFunction = new Spy(configManager);
        this.filter = new Filter(configManager);
        this.linkBlocker = new LinkBlocker(configManager);
        this.antiSpam = new AntiSpam(configManager, playerTimeManager);
        this.playerInfoManager = new PlayerInfoManager(configManager, playerTimeManager);
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        String message = event.getMessage();
        String prefix = configManager.getChatPrefix();

        boolean hasBypass = player.hasPermission("fcchat.bypass");

        if (linkBlocker.isBlocked(message) && !hasBypass) {
            event.setCancelled(true);
            player.sendMessage(ChatColor.translateAlternateColorCodes('&', linkBlocker.getBlockedMessage()));
            return;
        }

        if (message.contains("%") && message.indexOf("%") != message.lastIndexOf("%")) {
            event.setCancelled(true);
            return;
        }

        if (message.contains("||") && !player.hasPermission(configManager.getHiddenTextPermission())) {
            event.setCancelled(true);
            return;
        }

        if (antiSpam.isSpam(player) && !hasBypass) {
            event.setCancelled(true);
            double remainingTime = antiSpam.getRemainingSpamTime(player);
            player.sendMessage(antiSpam.getAntiSpamMessage(remainingTime));
            return;
        }

        if (antiSpam.isNewPlayerBlocked(player) && !hasBypass) {
            event.setCancelled(true);
            double remainingTime = antiSpam.getRemainingNewPlayerTime(player);
            player.sendMessage(antiSpam.getNewPlayerMessage(remainingTime));
            return;
        }

        event.setCancelled(true);

        String currentChannel = channelManager.getPlayerChannel(player.getUniqueId());
        if (!currentChannel.equals("default")) {
            channelManager.handleChannelChat(player, message);
            return;
        }

        if (message.startsWith(prefix)) {
            String chatMessage = message.substring(prefix.length());
            
            if (chatMessage.trim().isEmpty()) {
                return;
            }

            if (chatMessage.startsWith(" ")) {
                handleLocalChat(player, chatMessage.substring(1));
                String formattedConsoleMessage = formatMessage(configManager.getLocalChatFormat(), player, chatMessage.substring(1), true);
                plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', formattedConsoleMessage));
            } else {
                handleGlobalChat(player, chatMessage);
                String formattedConsoleMessage = formatMessage(configManager.getGlobalChatFormat(), player, chatMessage, true);
                plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', formattedConsoleMessage));
            }
        } else {
            handleLocalChat(player, message);
            String formattedConsoleMessage = formatMessage(configManager.getLocalChatFormat(), player, message, true);
            plugin.getLogger().info(ChatColor.translateAlternateColorCodes('&', formattedConsoleMessage));
        }
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        playerTimeManager.onPlayerJoin(event.getPlayer());
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        playerTimeManager.onPlayerQuit(event.getPlayer());
    }

    public void reloadModeration() {
        filter.reloadFilter();
        linkBlocker.reloadModeration();
    }

    public ChannelManager getChannelManager() {
        return channelManager;
    }

    public Spy getSpyFunction() {
        return spyFunction;
    }

        private void handleLocalChat(Player sender, String message) {
        String filteredMessage = filter.filterMessage(message, sender);
        String processedMessage = MessageProcessor.processHiddenText(filteredMessage, configManager);
        String formattedMessage = formatMessage(configManager.getLocalChatFormat(), sender, processedMessage, false);
        int radius = configManager.getLocalChatRadius();
        Location senderLocation = sender.getLocation();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.getWorld().equals(sender.getWorld()) &&
                player.getLocation().distance(senderLocation) <= radius) {
                
                if (shouldHideMessage(sender, player)) {
                    continue;
                }
                TextComponent finalComponent = createFinalMessageComponent(formattedMessage, filteredMessage, message, sender, player);
                player.spigot().sendMessage(finalComponent);
            }
        }

        DiscordIntegration discord = configManager.getDiscordIntegration();
        if (discord.isEnabled()) {
            discord.sendMessage(sender, filteredMessage);
        }

        if (configManager.isSpyEnabled()) {
            spyFunction.sendSpyMessage(sender, filteredMessage, formattedMessage);
        }
    }

    private void handleGlobalChat(Player sender, String message) {
        String filteredMessage = filter.filterMessage(message, sender);
        String processedMessage = MessageProcessor.processHiddenText(filteredMessage, configManager);
        String formattedMessage = formatMessage(configManager.getGlobalChatFormat(), sender, processedMessage, false);

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (shouldHideMessage(sender, player)) {
                continue;
            }

            TextComponent finalComponent = createFinalMessageComponent(formattedMessage, filteredMessage, message, sender, player);
            player.spigot().sendMessage(finalComponent);
        }

        DiscordIntegration discord = configManager.getDiscordIntegration();
        if (discord.isEnabled()) {
            discord.sendMessage(sender, filteredMessage);
        }
    }

    private boolean shouldHideMessage(Player sender, Player receiver) {
        return false;
    }

    private TextComponent createFinalMessageComponent(String formattedMessage, String filteredMessage, String originalMessage, Player sender, Player receiver) {
        String processedMessage = MessageProcessor.processHiddenText(filteredMessage, configManager);
        String finalFormattedMessage = formattedMessage;
        
        boolean hasHiddenText = filteredMessage.contains("||") && sender.hasPermission(configManager.getHiddenTextPermission());
        boolean wasFiltered = filter.wasMessageFiltered(originalMessage, filteredMessage);
        boolean canReadBlocked = receiver.hasPermission("fcchat.read");
        boolean canSeePlayerInfo = receiver.hasPermission(configManager.getPlayerInfoPermission()) && configManager.isPlayerInfoEnabled();
        
        if (canSeePlayerInfo) {
            return createPlayerInfoMessage(formattedMessage, filteredMessage, originalMessage, sender, receiver, hasHiddenText, wasFiltered, canReadBlocked);
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
                messageComponent = copyFunction.addClickEvent(messageComponent, filteredMessage);
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
                component = copyFunction.createClickableMessage(formattedMessage, filteredMessage);
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
    
    private TextComponent createPlayerInfoMessage(String formattedMessage, String filteredMessage, String originalMessage, Player sender, Player receiver, boolean hasHiddenText, boolean wasFiltered, boolean canReadBlocked) {
        String processedMessage = MessageProcessor.processHiddenText(filteredMessage, configManager);
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
                hiddenComponent = copyFunction.addClickEvent(hiddenComponent, filteredMessage);
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
                beforeMsgComponent = copyFunction.addClickEvent(beforeMsgComponent, filteredMessage);
                messageComponent = copyFunction.addClickEvent(messageComponent, filteredMessage);
                afterMsgComponent = copyFunction.addClickEvent(afterMsgComponent, filteredMessage);
            }
            
            beforeComponent.addExtra(playerComponent);
            beforeComponent.addExtra(beforeMsgComponent);
            beforeComponent.addExtra(messageComponent);
            beforeComponent.addExtra(afterMsgComponent);
            return beforeComponent;
        } else {
            TextComponent afterComponent = new TextComponent(afterPlayer);
            
            if (receiver.hasPermission(configManager.getCopyPermission()) && configManager.isCopyEnabled()) {
                afterComponent = copyFunction.addClickEvent(afterComponent, filteredMessage);
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

    private boolean isMessageBlocked(String message, Player sender) {
        if (filter.isBlocked(message)) {
            return true;
        }
        
        if (linkBlocker.isBlocked(message)) {
            return true;
        }
        
        if (antiSpam.isSpam(sender) && !sender.hasPermission("fcchat.bypass")) {
            return true;
        }
        
        if (antiSpam.isNewPlayerBlocked(sender) && !sender.hasPermission("fcchat.bypass")) {
            return true;
        }
        
        return false;
    }

    private String formatMessage(String format, Player player, String message, boolean usePlaceholders) {
        String formattedMessage = format
                .replace("{player}", player.getName())
                .replace("{message}", processMessageColors(message, player));

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