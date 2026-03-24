package fc.plugins.fcchat;

import fc.plugins.fcchat.chat.automessages.AutoMessages;
import fc.plugins.fcchat.chat.ChatManager;
import fc.plugins.fcchat.utils.function.chatgame.ChatGame;
import fc.plugins.fcchat.commands.ChatCommands;
import fc.plugins.fcchat.commands.ChatTabCompleter;
import fc.plugins.fcchat.commands.ClearCommand;
import fc.plugins.fcchat.commands.ChannelCommand;
import fc.plugins.fcchat.commands.PrivateMessageCommand;
import fc.plugins.fcchat.commands.PrivateMessageTabCompleter;
import fc.plugins.fcchat.commands.ReplyCommand;
import fc.plugins.fcchat.commands.BroadcastCommand;
import fc.plugins.fcchat.api.FcChatApi;
import fc.plugins.fcchat.api.FcChatApiProvider;
import fc.plugins.fcchat.api.event.FcChatApiReadyEvent;
import fc.plugins.fcchat.api.internal.FcChatApiImpl;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.data.PlayerTimeManager;
import fc.plugins.fcchat.integration.database.MySQLManager;
import fc.plugins.fcchat.manager.CooldownManager;
import fc.plugins.fcchat.manager.PrivateMessageManager;
import fc.plugins.fcchat.manager.PrivateMessageSoundManager;
import fc.plugins.fcchat.manager.ActionManager;

import fc.plugins.fcchat.manager.holograms.HologramsManager;
import fc.plugins.fcchat.integration.DiscordWebhook;
import fc.plugins.fcchat.chat.listeners.JoinLeaveListener;
import fc.plugins.fcchat.chat.listeners.UpdateListener;
import fc.plugins.fcchat.moderation.AiModerator;
import fc.plugins.fcchat.utils.sync.MessageSynchronizer;
import fc.plugins.fcchat.utils.Metrics;
import fc.plugins.fcchat.utils.Updater;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;

public final class FcChat
        extends JavaPlugin {
    private static final int BSTATS_PLUGIN_ID = 26800;
    private ConfigManager configManager;
    private PlayerTimeManager playerTimeManager;
    private ChatManager chatManager;
    private ChatCommands chatCommands;
    private ChatTabCompleter chatTabCompleter;
    private ClearCommand clearCommand;
    private ChannelCommand channelCommand;
    private PrivateMessageCommand privateMessageCommand;
    private PrivateMessageTabCompleter privateMessageTabCompleter;
    private ReplyCommand replyCommand;
    private BroadcastCommand broadcastCommand;
    private PrivateMessageManager privateMessageManager;
    private CooldownManager cooldownManager;
    private PrivateMessageSoundManager privateMessageSoundManager;
    private ActionManager actionManager;

    private AutoMessages autoMessages;
    private Updater updater;
    private ChatGame chatGame;
    private MySQLManager mysqlManager;
    private MessageSynchronizer messageSynchronizer;
    private HologramsManager hologramsManager;
    private DiscordWebhook discordWebhook;
    private AiModerator aiModerator;
    private FcChatApi api;

    public void onEnable() {
        try {
            this.configManager = new ConfigManager(this);
            this.getLogger().info("§7# # # # # # # # # # # # # # # # # # # # # # #");
            this.getLogger().info("§7#                                           #");
            this.getLogger().info("§7#       Плагин был создан студией           #");
            this.getLogger().info("§7#              fcPlugins                    #");
            this.getLogger().info("§7#    https://t.me/fcplugins_minecraft       #");
            this.getLogger().info("§7#                                           #");
            this.getLogger().info("§7# # # # # # # # # # # # # # # # # # # # # # #");
            this.playerTimeManager = new PlayerTimeManager();

            this.mysqlManager = new MySQLManager(this);
            this.messageSynchronizer = new MessageSynchronizer(this, this.mysqlManager);
            this.hologramsManager = new HologramsManager(this);
            this.discordWebhook = new DiscordWebhook(this, this.configManager);
            this.aiModerator = new AiModerator(this.configManager);
            this.actionManager = new ActionManager(this);
            this.chatManager = new ChatManager(this, this.configManager, this.playerTimeManager, this.messageSynchronizer, this.hologramsManager, this.actionManager);
            this.chatCommands = new ChatCommands(this, this.configManager);
            this.chatTabCompleter = new ChatTabCompleter(this.configManager, this);
            this.clearCommand = new ClearCommand(this, this.configManager);
            this.channelCommand = new ChannelCommand(this, this.configManager);
            this.privateMessageManager = new PrivateMessageManager();
            this.cooldownManager = new CooldownManager(this.configManager);
            this.privateMessageSoundManager = new PrivateMessageSoundManager(this.configManager);
            this.privateMessageCommand = new PrivateMessageCommand(this, this.configManager);
            this.privateMessageTabCompleter = new PrivateMessageTabCompleter();
            this.replyCommand = new ReplyCommand(this, this.configManager);
            this.broadcastCommand = new BroadcastCommand(this, this.configManager);
            this.api = new FcChatApiImpl(this, this.configManager);
            FcChatApiProvider.set(this.api);
            this.getServer().getServicesManager().register(FcChatApi.class, this.api, this, ServicePriority.Normal);
            this.autoMessages = new AutoMessages(this);
            this.updater = new Updater(this);
            this.chatGame = new ChatGame(this);
            new Metrics(this, BSTATS_PLUGIN_ID);
            this.getServer().getPluginManager().callEvent(new FcChatApiReadyEvent(this.api, false));
            this.registerEventsWithPriority();
            this.getServer().getPluginManager().registerEvents(new UpdateListener(this), this);
            this.getServer().getPluginManager().registerEvents(new JoinLeaveListener(this.configManager), this);
            this.getCommand("fcchat").setExecutor(this.chatCommands);
            this.getCommand("fcchat").setTabCompleter(this.chatTabCompleter);
            if (this.configManager.isCommandEnabled("clear")) {
                this.getCommand("clear").setExecutor(this.clearCommand);
            }
            this.getCommand("channel").setExecutor(this.channelCommand);
            if (this.configManager.isCommandEnabled("msg")) {
                this.getCommand("msg").setExecutor(this.privateMessageCommand);
                this.getCommand("msg").setTabCompleter(this.privateMessageTabCompleter);
                if (this.configManager.isCommandAliasEnabled("msg", "tell")) {
                    this.getCommand("tell").setExecutor(this.privateMessageCommand);
                    this.getCommand("tell").setTabCompleter(this.privateMessageTabCompleter);
                }
                if (this.configManager.isCommandAliasEnabled("msg", "whisper")) {
                    this.getCommand("whisper").setExecutor(this.privateMessageCommand);
                    this.getCommand("whisper").setTabCompleter(this.privateMessageTabCompleter);
                }
                if (this.configManager.isCommandAliasEnabled("msg", "w")) {
                    this.getCommand("w").setExecutor(this.privateMessageCommand);
                    this.getCommand("w").setTabCompleter(this.privateMessageTabCompleter);
                }
            }
            if (this.configManager.isCommandEnabled("reply")) {
                if (this.configManager.isCommandAliasEnabled("reply", "r")) {
                    this.getCommand("r").setExecutor(this.replyCommand);
                }
                this.getCommand("reply").setExecutor(this.replyCommand);
            }
            if (this.configManager.isCommandEnabled("broadcast")) {
                this.getCommand("broadcast").setExecutor(this.broadcastCommand);
                if (this.configManager.isCommandAliasEnabled("broadcast", "bc")) {
                    this.getCommand("bc").setExecutor(this.broadcastCommand);
                }
                if (this.configManager.isCommandAliasEnabled("broadcast", "announce")) {
                    this.getCommand("announce").setExecutor(this.broadcastCommand);
                }
            }
            this.autoMessages.start();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ChatManager getChatManager() {
        return this.chatManager;
    }

    public PlayerTimeManager getPlayerTimeManager() {
        return this.playerTimeManager;
    }


    public AutoMessages getAutoMessages() {
        return this.autoMessages;
    }

    public Updater getUpdater() {
        return this.updater;
    }

    public ConfigManager getConfigManager() {
        return this.configManager;
    }

    public ChatGame getChatGame() {
        return this.chatGame;
    }

    public void onDisable() {
        try {
            if (this.autoMessages != null) {
                this.autoMessages.stop();
            }
            if (this.chatGame != null) {
                this.chatGame.stop();
            }
            if (this.messageSynchronizer != null) {
                this.messageSynchronizer.stop();
            }
            if (this.mysqlManager != null) {
                this.mysqlManager.disconnect();
            }
            if (this.hologramsManager != null) {
                this.hologramsManager.removeAllHolograms();
            }
            if (this.discordWebhook != null) {
                this.discordWebhook.disable();
            }
            if (this.api != null) {
                this.getServer().getServicesManager().unregister(FcChatApi.class, this.api);
                FcChatApiProvider.set(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public MessageSynchronizer getMessageSynchronizer() {
        return this.messageSynchronizer;
    }

    public MySQLManager getMySQLManager() {
        return this.mysqlManager;
    }

    public HologramsManager getHologramManager() {
        return this.hologramsManager;
    }

    public PrivateMessageManager getPrivateMessageManager() {
        return this.privateMessageManager;
    }

    public CooldownManager getCooldownManager() {
        return this.cooldownManager;
    }

    public PrivateMessageSoundManager getPrivateMessageSoundManager() {
        return this.privateMessageSoundManager;
    }

    public DiscordWebhook getDiscordWebhook() {
        return this.discordWebhook;
    }

    public ActionManager getActionManager() {
        return this.actionManager;
    }

    public FcChatApi getApi() {
        return this.api;
    }

    public AiModerator getAiModerator() {
        return this.aiModerator;
    }

    private void registerEventsWithPriority() {
        EventPriority priority = getEventPriorityFromConfig();
        this.getServer().getPluginManager().registerEvent(
            org.bukkit.event.player.AsyncPlayerChatEvent.class,
            this.chatManager,
            priority,
            (listener, event) -> {
                if (event instanceof org.bukkit.event.player.AsyncPlayerChatEvent) {
                    this.chatManager.onPlayerChat((org.bukkit.event.player.AsyncPlayerChatEvent) event);
                }
            },
            this
        );
        this.getServer().getPluginManager().registerEvents(this.chatGame, this);
    }

    private EventPriority getEventPriorityFromConfig() {
        String priorityString = this.configManager.getEventPriority().toUpperCase();
        try {
            return EventPriority.valueOf(priorityString);
        } catch (IllegalArgumentException e) {
            return EventPriority.NORMAL;
        }
    }

    public void reloadEventsWithPriority() {
        HandlerList.unregisterAll(this.chatManager);
        registerEventsWithPriority();
    }
}
