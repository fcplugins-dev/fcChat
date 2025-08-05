package fc.plugins.fcchat;

import fc.plugins.fcchat.automessages.AutoMessages;
import fc.plugins.fcchat.chat.ChatManager;
import fc.plugins.fcchat.commands.ChatCommands;
import fc.plugins.fcchat.commands.ChatTabCompleter;
import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.data.PlayerTimeManager;
import fc.plugins.fcchat.function.Copy;
import org.bukkit.plugin.java.JavaPlugin;

public final class FcChat extends JavaPlugin {
    private ConfigManager configManager;
    private PlayerTimeManager playerTimeManager;
    private ChatManager chatManager;
    private ChatCommands chatCommands;
    private ChatTabCompleter chatTabCompleter;
    private Copy copyFunction;
    private AutoMessages autoMessages;

    @Override
    public void onEnable() {
        configManager = new ConfigManager(this);
        playerTimeManager = new PlayerTimeManager(this);
        copyFunction = new Copy(configManager);
        chatManager = new ChatManager(this, configManager, playerTimeManager);
        chatCommands = new ChatCommands(this, configManager);
        chatTabCompleter = new ChatTabCompleter(configManager, this);
        autoMessages = new AutoMessages(this);

        getServer().getPluginManager().registerEvents(chatManager, this);
        getCommand("fcchat").setExecutor(chatCommands);
        getCommand("fcchat").setTabCompleter(chatTabCompleter);

        autoMessages.start();

        getLogger().info("FcChat успешно загружен!");
    }

    public ChatManager getChatManager() {
        return chatManager;
    }

    public AutoMessages getAutoMessages() {
        return autoMessages;
    }

    @Override
    public void onDisable() {
        if (playerTimeManager != null) {
            playerTimeManager.saveAllData();
        }
        if (autoMessages != null) {
            autoMessages.stop();
        }
        getLogger().info("FcChat выключен!");
    }
} 