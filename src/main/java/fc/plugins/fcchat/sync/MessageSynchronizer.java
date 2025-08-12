package fc.plugins.fcchat.sync;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.database.MySQLManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.entity.Player;
import org.bukkit.Bukkit;

public class MessageSynchronizer {
    private final FcChat plugin;
    private final MySQLManager mysqlManager;
    private long lastMessageId = 0;
    private int taskId = -1;

    public MessageSynchronizer(FcChat plugin, MySQLManager mysqlManager) {
        this.plugin = plugin;
        this.mysqlManager = mysqlManager;
        startListening();
    }

    public void syncMessage(Player player, String message, String channelType) {
        if (!mysqlManager.isEnabled() || !mysqlManager.isSyncEnabled()) {
            return;
        }

        if (!mysqlManager.shouldSyncChannel(channelType)) {
            return;
        }

        mysqlManager.saveMessage(
            player.getName(),
            player.getUniqueId().toString(),
            message,
            channelType
        );
    }

    public void syncGlobalMessage(Player player, String message) {
        syncMessage(player, message, "global");
    }

    private boolean isCommand(String message) {
        return message.startsWith("!");
    }

    public boolean isEnabled() {
        return mysqlManager.isEnabled() && mysqlManager.isSyncEnabled();
    }

    private void startListening() {
        if (!isEnabled()) {
            return;
        }

        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            lastMessageId = mysqlManager.getLastMessageId();
        });
        
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            if (mysqlManager.isEnabled() && mysqlManager.isSyncEnabled()) {
                mysqlManager.getNewMessages(lastMessageId, this::processIncomingMessages);
            }
        }, 10L, 6L).getTaskId();
    }

    private void processIncomingMessages(java.util.List<MySQLManager.SyncMessage> messages) {
        if (!messages.isEmpty()) {
        }
        
        for (MySQLManager.SyncMessage syncMessage : messages) {
            
            if (syncMessage.id > lastMessageId) {
                lastMessageId = syncMessage.id;
            }

            if (!mysqlManager.shouldSyncChannel(syncMessage.channelType)) {
                continue;
            }

            String formattedMessage = createNormalMessage(syncMessage);
            
            if (syncMessage.channelType.equals("global")) {
                Bukkit.broadcastMessage(formattedMessage);
            }
        }
    }

    private String createNormalMessage(MySQLManager.SyncMessage syncMessage) {
        String format;
        if (syncMessage.channelType.equals("global")) {
            format = plugin.getConfigManager().getGlobalChatFormat();
        } else {
            format = "&f{player}&7: &f{message}";
        }
        String formattedMessage = format
            .replace("{player}", syncMessage.playerName)
            .replace("{message}", syncMessage.message)
            .replace("%player_name%", syncMessage.playerName);
        formattedMessage = formattedMessage
            .replace("%prefix%", "")
            .replace("%suffix%", "")
            .replace("%luckperms_prefix%", "")
            .replace("%luckperms_suffix%", "");
            
        return HexUtils.translateAlternateColorCodes(formattedMessage);
    }

    private void broadcastToChannel(String message, String channelName) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (plugin.getChatManager().getChannelManager().hasChannelPermission(player, channelName)) {
                player.sendMessage(message);
            }
        }
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = -1;
        }
    }

    public void setLastMessageId(long id) {
        this.lastMessageId = id;
    }
}