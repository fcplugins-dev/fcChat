package fc.plugins.fcchat.api;

import fc.plugins.fcchat.api.actions.ActionExecutionContext;
import fc.plugins.fcchat.api.actions.ActionHandler;
import fc.plugins.fcchat.api.audience.AudienceResolver;
import fc.plugins.fcchat.api.placeholder.ContextPlaceholderProvider;
import fc.plugins.fcchat.chat.channel.Channel;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

public interface FcChatApi {
    boolean sendBroadcast(CommandSender sender, String message);

    boolean sendPrivateMessage(Player sender, Player receiver, String message);

    boolean sendReplyMessage(Player sender, Player receiver, String message);

    void clearChat(CommandSender sender, boolean announce);

    boolean switchChannel(Player player, String channelId);

    String getPlayerChannel(UUID playerId);

    Map<String, Channel> getChannels();

    boolean isCommandEnabled(String command);

    void registerContextPlaceholder(String key, ContextPlaceholderProvider provider);

    void unregisterContextPlaceholder(String key);

    String applyContextPlaceholders(CommandSender sender, Player viewer, String text, String rawMessage);

    void registerAudienceResolver(AudienceResolver resolver);

    void unregisterAudienceResolver(String resolverId);

    Set<Player> resolveBroadcastRecipients(CommandSender sender, String message, Set<Player> defaultRecipients);

    void registerActionHandler(String actionId, ActionHandler handler);

    void unregisterActionHandler(String actionId);

    boolean executeAction(String actionId, ActionExecutionContext context);
}
