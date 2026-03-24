package fc.plugins.fcchat.utils.sync;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.integration.database.MySQLManager;
import fc.plugins.fcchat.integration.database.MySQLManager.SyncMessage;
import fc.plugins.fcchat.utils.HexUtils;
import java.util.Iterator;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class MessageSynchronizer {
    private final FcChat plugin;
    private final MySQLManager mysqlManager;
    private long lastMessageId = 0L;
    private int taskId = -1;

    public MessageSynchronizer(FcChat plugin, MySQLManager mysqlManager) {
        this.plugin = plugin;
        this.mysqlManager = mysqlManager;
        this.startListening();
    }

    public void syncMessage(Player player, String message, String channelType) {
        if (this.mysqlManager.isEnabled() && this.mysqlManager.isSyncEnabled()) {
            if (this.mysqlManager.shouldSyncChannel(channelType)) {
                this.mysqlManager.saveMessage(player.getName(), player.getUniqueId().toString(), message, channelType);
            }
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
            Bukkit.getScheduler().runTaskAsynchronously(this.plugin, () -> {
                this.lastMessageId = this.mysqlManager.getLastMessageId();
            });
            this.taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(this.plugin, () -> {
                if (this.mysqlManager.isEnabled() && this.mysqlManager.isSyncEnabled()) {
                    this.mysqlManager.getNewMessages(this.lastMessageId, this::processIncomingMessages);
                }

            }, 10L, 6L).getTaskId();
        }
    }

    private void processIncomingMessages(List<SyncMessage> messages) {
        if (!messages.isEmpty()) {
        }

        Iterator var2 = messages.iterator();

        while(var2.hasNext()) {
            SyncMessage syncMessage = (SyncMessage)var2.next();
            if (syncMessage.id > this.lastMessageId) {
                this.lastMessageId = syncMessage.id;
            }

            if (this.mysqlManager.shouldSyncChannel(syncMessage.channelType)) {
                String formattedMessage = this.createNormalMessage(syncMessage);
                if (syncMessage.channelType.equals("global")) {
                    Bukkit.broadcastMessage(formattedMessage);
                }
            }
        }

    }

    private String createNormalMessage(SyncMessage syncMessage) {
        String format;
        if (syncMessage.channelType.equals("global")) {
            format = this.plugin.getConfigManager().getGlobalChatFormat();
        } else {
            format = "{player}&7: &f{message}";
        }

        String formattedMessage = format.replace("{player}", syncMessage.playerName).replace("{message}", syncMessage.message).replace("%player_name%", syncMessage.playerName);
        formattedMessage = formattedMessage.replace("%prefix%", "").replace("%suffix%", "").replace("%luckperms_prefix%", "").replace("%luckperms_suffix%", "");
        return HexUtils.translateAlternateColorCodes(formattedMessage);
    }

    private void broadcastToChannel(String message, String channelName) {
        Iterator var3 = Bukkit.getOnlinePlayers().iterator();

        while(var3.hasNext()) {
            Player player = (Player)var3.next();
            if (this.plugin.getChatManager().getChannelManager().hasChannelPermission(player, channelName)) {
                player.sendMessage(message);
            }
        }

    }

    public void stop() {
        if (this.taskId != -1) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = -1;
        }

    }

    public void setLastMessageId(long id) {
        this.lastMessageId = id;
    }
}