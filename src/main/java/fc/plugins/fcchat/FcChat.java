package fc.plugins.fcchat;

import fc.plugins.fcchat.automessages.AutoMessages;
import fc.plugins.fcchat.chat.ChatManager;
import fc.plugins.fcchat.chatgame.ChatGame;
import fc.plugins.fcchat.commands.ChatCommands;
import fc.plugins.fcchat.commands.ChatTabCompleter;
import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.data.PlayerTimeManager;
import fc.plugins.fcchat.function.Copy;
import fc.plugins.fcchat.listeners.UpdateListener;
import fc.plugins.fcchat.listeners.JoinLeaveListener;
import fc.plugins.fcchat.database.MySQLManager;
import fc.plugins.fcchat.sync.MessageSynchronizer;
import fc.plugins.fcchat.utils.Metrics;
import fc.plugins.fcchat.holograms.HologramsManager;
import fc.plugins.fcchat.utils.Updater;
import org.bukkit.plugin.java.JavaPlugin;

public final class FcChat extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 26800;
    
    private ConfigManager configManager;
    private PlayerTimeManager playerTimeManager;
    private ChatManager chatManager;
    private ChatCommands chatCommands;
    private ChatTabCompleter chatTabCompleter;
    private Copy copyFunction;
    private AutoMessages autoMessages;
    private Updater updater;
    private ChatGame chatGame;
    private MySQLManager mysqlManager;
    private MessageSynchronizer messageSynchronizer;
    private HologramsManager hologramsManager;

    @Override
    public void onEnable() {
        try {
            configManager = new ConfigManager(this);
            getLogger().info("§7# # # # # # # # # # # # # # # # # # # # # # #");
            getLogger().info("§7#                                           #");
            getLogger().info("§7#       Plugin developed by                 #");
            getLogger().info("§7#            fcPlugins                      #");
            getLogger().info("§7#    https://t.me/fcplugins_minecraft       #");
            getLogger().info("§7#                                           #");
            getLogger().info("§7# # # # # # # # # # # # # # # # # # # # # # #");
            
            playerTimeManager = new PlayerTimeManager(this);
            copyFunction = new Copy(configManager);
            mysqlManager = new MySQLManager(this);
            messageSynchronizer = new MessageSynchronizer(this, mysqlManager);
            hologramsManager = new HologramsManager(this);
            chatManager = new ChatManager(this, configManager, playerTimeManager, messageSynchronizer, hologramsManager);
            chatCommands = new ChatCommands(this, configManager);
            chatTabCompleter = new ChatTabCompleter(configManager, this);
            autoMessages = new AutoMessages(this);
            updater = new Updater(this);
            chatGame = new ChatGame(this);
            new Metrics(this, BSTATS_PLUGIN_ID);

            getServer().getPluginManager().registerEvents(chatManager, this);
            getServer().getPluginManager().registerEvents(chatGame, this);
            getServer().getPluginManager().registerEvents(new UpdateListener(this), this);
            getServer().getPluginManager().registerEvents(new JoinLeaveListener(configManager), this);
            getCommand("fcchat").setExecutor(chatCommands);
            getCommand("fcchat").setTabCompleter(chatTabCompleter);

            autoMessages.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public AutoMessages getAutoMessages() {
        return autoMessages;
    }

    public Updater getUpdater() {
        return updater;
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public ChatGame getChatGame() {
        return chatGame;
    }

    @Override
    public void onDisable() {
        try {
            if (playerTimeManager != null) {
                playerTimeManager.saveAllData();
            }
            if (autoMessages != null) {
                autoMessages.stop();
            }
            if (chatGame != null) {
                chatGame.stop();
            }
            if (messageSynchronizer != null) {
                messageSynchronizer.stop();
            }
            if (mysqlManager != null) {
                mysqlManager.disconnect();
            }
            if (hologramsManager != null) {
                hologramsManager.removeAllHolograms();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MessageSynchronizer getMessageSynchronizer() {
        return messageSynchronizer;
    }

    public MySQLManager getMySQLManager() {
        return mysqlManager;
    }
    
    public HologramsManager getHologramManager() {
        return hologramsManager;
    }
} 