package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommands
        implements CommandExecutor {
    private final FcChat plugin;
    private final ConfigManager configManager;

    public ChatCommands(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fcchat")) {
            if (args.length == 0) {
                sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("unknown-command")));
                return true;
            }
            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("fcchat.reload")) {
                    this.configManager.reloadConfig();
                    if (this.plugin.getAiModerator() != null) {
                        this.plugin.getAiModerator().reload();
                    }
                    this.plugin.getAutoMessages().reload();
                    this.plugin.getChatManager().getChannelManager().reloadChannels();
                    this.plugin.getChatGame().reloadConfig();
                    this.plugin.getActionManager().reloadActionConfig();
                    this.plugin.reloadEventsWithPriority();
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("reload-success")));
                } else {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("no-permission")));
                }
                return true;
            }
            if (args[0].equalsIgnoreCase("ai")) {
                if (!sender.hasPermission("fcchat.ai.manage")) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("no-permission")));
                    return true;
                }

                if (args.length < 2) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.msg("ai-moderator.usage", "&6[⚠] &fUsage&6: /&ffcchat ai &6<&fstats/logger&6>")));
                    return true;
                }

                if (args[1].equalsIgnoreCase("stats")) {
                    long blockedCount = this.plugin.getAiModerator().getBlockedMessagesCount();
                    String loggerOn = this.msg("ai-moderator.status.on", "&aON");
                    String loggerOff = this.msg("ai-moderator.status.off", "&cOFF");
                    String loggerStatus = this.plugin.getAiModerator().isLoggerEnabled() ? loggerOn : loggerOff;
                    String message = this.msg("ai-moderator.stats", "&6[AI] &fBlocked messages&6: &f{blocked} &7| &fLogger&6: {logger}")
                            .replace("{blocked}", String.valueOf(blockedCount))
                            .replace("{logger}", loggerStatus);
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(message));
                    return true;
                }

                if (args[1].equalsIgnoreCase("logger")) {
                    boolean enabled = this.plugin.getAiModerator().toggleLogger();
                    String message = enabled
                            ? this.msg("ai-moderator.logger-enabled", "&a[✔] &fAI logger enabled&a!")
                            : this.msg("ai-moderator.logger-disabled", "&6[⚠] &fAI logger disabled&6!");
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(message));
                    return true;
                }

                sender.sendMessage(HexUtils.translateAlternateColorCodes(this.msg("ai-moderator.usage", "&6[⚠] &fUsage&6: /&ffcchat ai &6<&fstats/logger&6>")));
                return true;
            }
            if (args[0].equalsIgnoreCase("channel")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.players-only")));
                    return true;
                }
                Player player = (Player)((Object)sender);
                if (args.length < 2) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.usage")));
                    return true;
                }
                String channelId = args[1];
                if (channelId.equalsIgnoreCase("default")) {
                    this.plugin.getApi().switchChannel(player, "default");
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.switched-default")));
                    return true;
                }

                if (this.plugin.getChatManager().getChannelManager().getChannel(channelId) == null) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.not-found")));
                    return true;
                }

                if (!this.plugin.getChatManager().getChannelManager().hasChannelPermission(player, channelId)) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.no-permission")));
                    return true;
                }

                if (!this.plugin.getApi().switchChannel(player, channelId)) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.switch-cancelled")));
                    return true;
                }

                String message = this.configManager.getMessage("channel.switched").replace("{channel}", channelId);
                sender.sendMessage(HexUtils.translateAlternateColorCodes(message));
                return true;
            }
            if (args[0].equalsIgnoreCase("clear")) {
                if (!this.configManager.isCommandEnabled("clear")) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("unknown-command")));
                    return true;
                }
                if (!sender.hasPermission("fcchat.clear")) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("no-permission")));
                    return true;
                }
                this.plugin.getApi().clearChat(sender, true);
                return true;
            }
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("unknown-command")));
            return true;
        }
        return false;
    }

    private String msg(String path, String fallback) {
        String value = this.configManager.getMessage(path);
        return value == null ? fallback : value;
    }
}
