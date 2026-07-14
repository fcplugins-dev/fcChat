
package fc.plugins.fcchat.api;

import fc.plugins.fcchat.api.actions.ActionExecutionContext;
import fc.plugins.fcchat.api.actions.ActionHandler;
import fc.plugins.fcchat.api.audience.AudienceResolver;
import fc.plugins.fcchat.api.placeholder.ContextPlaceholderProvider;
import fc.plugins.fcchat.chat.channel.Channel;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface FcChatApi {
    public boolean sendBroadcast(CommandSender var1, String var2);

    public boolean sendPrivateMessage(Player var1, Player var2, String var3);

    public boolean sendReplyMessage(Player var1, Player var2, String var3);

    public void clearChat(CommandSender var1, boolean var2);

    public boolean switchChannel(Player var1, String var2);

    public String getPlayerChannel(UUID var1);

    public Map<String, Channel> getChannels();

    public boolean isCommandEnabled(String var1);

    public void registerContextPlaceholder(String var1, ContextPlaceholderProvider var2);

    public void unregisterContextPlaceholder(String var1);

    public String applyContextPlaceholders(CommandSender var1, Player var2, String var3, String var4);

    public void registerAudienceResolver(AudienceResolver var1);

    public void unregisterAudienceResolver(String var1);

    public Set<Player> resolveBroadcastRecipients(CommandSender var1, String var2, Set<Player> var3);

    public void registerActionHandler(String var1, ActionHandler var2);

    public void unregisterActionHandler(String var1);

    public boolean executeAction(String var1, ActionExecutionContext var2);
}

