
package fc.plugins.fcchat.utils.sync;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.integration.database.MySQLManager;
import fc.plugins.fcchat.utils.concurrent.CompatScheduler;
import fc.plugins.fcchat.utils.HexUtils;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MessageSynchronizer {
    private final FcChat plugin;
    private final MySQLManager mysqlManager;
    private long lastMessageId = 0L;
    private CompatScheduler.ScheduledTask pollingTask;

    public MessageSynchronizer(FcChat plugin, MySQLManager mysqlManager) {
        this.plugin = plugin;
        this.mysqlManager = mysqlManager;
        this.startListening();
    }

    public void syncMessage(Player player, String message, String channelType) {
        if (this.mysqlManager.isEnabled() && this.mysqlManager.isSyncEnabled() && this.mysqlManager.shouldSyncChannel(channelType)) {
            this.mysqlManager.saveMessage(player.getName(), player.getUniqueId().toString(), message, channelType);
        }
    }

    public void syncGlobalMessage(Player player, String message) {
        this.syncMessage(player, message, "global");
    }

    private boolean isCommand(String message) {
        return message.startsWith("!");
    }

    public boolean isEnabled() {
        return this.mysqlManager.isEnabled() && this.mysqlManager.isSyncEnabled();
    }

    private void startListening() {
        if (this.isEnabled()) {
            this.plugin.getCompatScheduler().runAsync(() -> {
                this.lastMessageId = this.mysqlManager.getLastMessageId();
            });
            this.pollingTask = this.plugin.getCompatScheduler().runAsyncTimer(10L, 6L, () -> {
                if (this.mysqlManager.isEnabled() && this.mysqlManager.isSyncEnabled()) {
                    this.mysqlManager.getNewMessages(this.lastMessageId, this::processIncomingMessages);
                }
            });
        }
    }

    private void processIncomingMessages(List<MySQLManager.SyncMessage> messages) {
        messages.isEmpty();
        for (MySQLManager.SyncMessage syncMessage : messages) {
            if (syncMessage.id > this.lastMessageId) {
                this.lastMessageId = syncMessage.id;
            }
            if (!this.mysqlManager.shouldSyncChannel(syncMessage.channelType)) continue;
            String formattedMessage = this.createNormalMessage(syncMessage);
            if (!syncMessage.channelType.equals("global")) continue;
            this.plugin.getCompatScheduler().runGlobal(() -> Bukkit.broadcastMessage(formattedMessage));
        }
    }

    private String createNormalMessage(MySQLManager.SyncMessage syncMessage) {
        String format = syncMessage.channelType.equals("global") ? this.plugin.getConfigManager().getGlobalChatFormat() : "{player}&7: &f{message}";
        String formattedMessage = format.replace("{player}", syncMessage.playerName).replace("{message}", syncMessage.message).replace("%player_name%", syncMessage.playerName);
        formattedMessage = formattedMessage.replace("%prefix%", "").replace("%suffix%", "").replace("%luckperms_prefix%", "").replace("%luckperms_suffix%", "");
        return HexUtils.translateAlternateColorCodes(formattedMessage);
    }

    private void broadcastToChannel(String message, String channelName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (!this.plugin.getChatManager().getChannelManager().hasChannelPermission(player, channelName)) continue;
            player.sendMessage(message);
        }
    }

    public void stop() {
        if (this.pollingTask != null) {
            this.pollingTask.cancel();
            this.pollingTask = null;
        }
    }

    public void setLastMessageId(long id) {
        this.lastMessageId = id;
    }
}
