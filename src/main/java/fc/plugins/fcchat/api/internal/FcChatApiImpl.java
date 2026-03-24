package fc.plugins.fcchat.api.internal;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.api.FcChatApi;
import fc.plugins.fcchat.api.actions.ActionExecutionContext;
import fc.plugins.fcchat.api.actions.ActionHandler;
import fc.plugins.fcchat.api.audience.AudienceResolver;
import fc.plugins.fcchat.api.event.FcChatBroadcastEvent;
import fc.plugins.fcchat.api.event.FcChatChannelSwitchEvent;
import fc.plugins.fcchat.api.event.FcChatPrivateMessageEvent;
import fc.plugins.fcchat.api.placeholder.ContextPlaceholderProvider;
import fc.plugins.fcchat.chat.channel.Channel;
import fc.plugins.fcchat.chat.channel.ChannelManager;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class FcChatApiImpl implements FcChatApi {
    private final FcChat plugin;
    private final ConfigManager configManager;
    private final Map<String, ContextPlaceholderProvider> contextPlaceholders;
    private final Map<String, AudienceResolver> audienceResolvers;
    private final Map<String, ActionHandler> actionHandlers;

    public FcChatApiImpl(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.contextPlaceholders = new ConcurrentHashMap<>();
        this.audienceResolvers = new ConcurrentHashMap<>();
        this.actionHandlers = new ConcurrentHashMap<>();
    }

    @Override
    public boolean sendBroadcast(CommandSender sender, String message) {
        String senderName = sender instanceof Player ? sender.getName() : "Console";
        String broadcastFormat = this.configManager.getPrivateMessageConfig().getString("broadcast.format", "&c[&4BROADCAST&c] &f{message}")
                .replace("{message}", message)
                .replace("{sender}", senderName)
                .replace("{player}", senderName);

        String formatted = applyContextPlaceholders(sender, null, broadcastFormat, message);
        Set<Player> recipients = resolveBroadcastRecipients(sender, message, new HashSet<>(Bukkit.getOnlinePlayers()));

        FcChatBroadcastEvent event = new FcChatBroadcastEvent(sender, message, formatted, recipients, !Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        for (Player player : event.getRecipients()) {
            String personalized = applyContextPlaceholders(sender, player, event.getFormattedMessage(), event.getMessage());
            player.sendMessage(HexUtils.translateAlternateColorCodes(personalized));
            this.plugin.getPrivateMessageSoundManager().playBroadcastSound(player);
        }
        return true;
    }

    @Override
    public boolean sendPrivateMessage(Player sender, Player receiver, String message) {
        return sendDirectMessage(sender, receiver, message, "private-messages", false);
    }

    @Override
    public boolean sendReplyMessage(Player sender, Player receiver, String message) {
        return sendDirectMessage(sender, receiver, message, "reply", true);
    }

    private boolean sendDirectMessage(Player sender, Player receiver, String message, String configPath, boolean replySound) {
        String senderFormat = this.configManager.getPrivateMessageConfig().getString(configPath + ".format.sender", "&7[&6You &7-> &e{receiver}&7] &f{message}")
                .replace("{receiver}", receiver.getName())
                .replace("{receiver_group}", this.getPlayerGroup(receiver))
                .replace("{message}", message)
                .replace("{sender}", sender.getName())
                .replace("{sender_group}", this.getPlayerGroup(sender));

        String receiverFormat = this.configManager.getPrivateMessageConfig().getString(configPath + ".format.receiver", "&7[&e{sender} &7-> &6You&7] &f{message}")
                .replace("{sender}", sender.getName())
                .replace("{sender_group}", this.getPlayerGroup(sender))
                .replace("{message}", message)
                .replace("{receiver}", receiver.getName())
                .replace("{receiver_group}", this.getPlayerGroup(receiver));

        senderFormat = applyContextPlaceholders(sender, sender, senderFormat, message);
        receiverFormat = applyContextPlaceholders(sender, receiver, receiverFormat, message);

        FcChatPrivateMessageEvent event = new FcChatPrivateMessageEvent(sender, receiver, message, senderFormat, receiverFormat, !Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        sender.sendMessage(HexUtils.translateAlternateColorCodes(event.getSenderFormat()));
        receiver.sendMessage(HexUtils.translateAlternateColorCodes(event.getReceiverFormat()));

        this.plugin.getPrivateMessageManager().setLastMessenger(receiver.getUniqueId(), sender.getUniqueId());

        if (replySound) {
            this.plugin.getPrivateMessageSoundManager().playReplySound(sender, "sender");
            this.plugin.getPrivateMessageSoundManager().playReplySound(receiver, "receiver");
        } else {
            this.plugin.getPrivateMessageSoundManager().playPrivateMessageSound(sender, "sender");
            this.plugin.getPrivateMessageSoundManager().playPrivateMessageSound(receiver, "receiver");
        }
        return true;
    }

    @Override
    public void clearChat(CommandSender sender, boolean announce) {
        for (int i = 0; i < 100; i++) {
            Bukkit.broadcastMessage("");
        }

        if (announce) {
            String clearMessage = this.configManager.getMessage("clear-chat").replace("{player}", sender.getName());
            clearMessage = applyContextPlaceholders(sender, null, clearMessage, "");
            Bukkit.broadcastMessage(HexUtils.translateAlternateColorCodes(clearMessage));
        }
    }

    @Override
    public boolean switchChannel(Player player, String channelId) {
        ChannelManager channelManager = this.plugin.getChatManager().getChannelManager();
        if (!"default".equalsIgnoreCase(channelId) && !channelManager.hasChannelPermission(player, channelId)) {
            return false;
        }

        String oldChannel = channelManager.getPlayerChannel(player.getUniqueId());
        String targetChannel = channelId == null ? "default" : channelId.toLowerCase();

        FcChatChannelSwitchEvent event = new FcChatChannelSwitchEvent(player, oldChannel, targetChannel, !Bukkit.isPrimaryThread());
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            return false;
        }

        channelManager.setPlayerChannel(player.getUniqueId(), targetChannel);
        return true;
    }

    @Override
    public String getPlayerChannel(UUID playerId) {
        return this.plugin.getChatManager().getChannelManager().getPlayerChannel(playerId);
    }

    @Override
    public Map<String, Channel> getChannels() {
        return Collections.unmodifiableMap(this.plugin.getChatManager().getChannelManager().getAllChannels());
    }

    @Override
    public boolean isCommandEnabled(String command) {
        return this.configManager.isCommandEnabled(command);
    }

    @Override
    public void registerContextPlaceholder(String key, ContextPlaceholderProvider provider) {
        if (key == null || provider == null) {
            return;
        }
        this.contextPlaceholders.put(key.toLowerCase(), provider);
    }

    @Override
    public void unregisterContextPlaceholder(String key) {
        if (key == null) {
            return;
        }
        this.contextPlaceholders.remove(key.toLowerCase());
    }

    @Override
    public String applyContextPlaceholders(CommandSender sender, Player viewer, String text, String rawMessage) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        String result = text;
        for (Map.Entry<String, ContextPlaceholderProvider> entry : this.contextPlaceholders.entrySet()) {
            String key = entry.getKey();
            String tokenA = "{ctx:" + key + "}";
            String tokenB = "{context:" + key + "}";

            if (!result.contains(tokenA) && !result.contains(tokenB)) {
                continue;
            }

            String replacement = "";
            try {
                String resolved = entry.getValue().resolve(sender, viewer, rawMessage);
                replacement = resolved == null ? "" : resolved;
            } catch (Exception ignored) {
            }

            result = result.replace(tokenA, replacement).replace(tokenB, replacement);
        }
        return result;
    }

    @Override
    public void registerAudienceResolver(AudienceResolver resolver) {
        if (resolver == null || resolver.getId() == null) {
            return;
        }
        this.audienceResolvers.put(resolver.getId().toLowerCase(), resolver);
    }

    @Override
    public void unregisterAudienceResolver(String resolverId) {
        if (resolverId == null) {
            return;
        }
        this.audienceResolvers.remove(resolverId.toLowerCase());
    }

    @Override
    public Set<Player> resolveBroadcastRecipients(CommandSender sender, String message, Set<Player> defaultRecipients) {
        Set<Player> recipients = new HashSet<>(defaultRecipients);
        List<AudienceResolver> resolvers = new ArrayList<>(this.audienceResolvers.values());
        resolvers.sort(Comparator.comparingInt(AudienceResolver::getPriority).reversed());

        for (AudienceResolver resolver : resolvers) {
            try {
                Set<Player> resolved = resolver.resolveRecipients(sender, message, Collections.unmodifiableSet(recipients));
                if (resolved != null) {
                    recipients = new HashSet<>(resolved);
                }
            } catch (Exception ignored) {
            }
        }
        return recipients;
    }

    @Override
    public void registerActionHandler(String actionId, ActionHandler handler) {
        if (actionId == null || handler == null) {
            return;
        }
        this.actionHandlers.put(actionId.toLowerCase(), handler);
    }

    @Override
    public void unregisterActionHandler(String actionId) {
        if (actionId == null) {
            return;
        }
        this.actionHandlers.remove(actionId.toLowerCase());
    }

    @Override
    public boolean executeAction(String actionId, ActionExecutionContext context) {
        if (actionId == null || context == null) {
            return false;
        }
        ActionHandler handler = this.actionHandlers.get(actionId.toLowerCase());
        if (handler == null) {
            return false;
        }

        try {
            handler.execute(context);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    private String getPlayerGroup(Player player) {
        try {
            return this.configManager.getLuckPermsIntegration().getPrimaryGroup(player, "default");
        } catch (Exception e) {
            return "default";
        }
    }
}
