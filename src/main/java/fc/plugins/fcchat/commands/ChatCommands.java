package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.function.Spy;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class  ChatCommands implements CommandExecutor {
    private final FcChat plugin;
    private final ConfigManager configManager;
    private final Spy spyFunction;

    public ChatCommands(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
        this.spyFunction = new Spy(configManager);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("fcchat")) {
            if (args.length == 0) {
                sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("unknown-command")));
                return true;
            }

            if (args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("fcchat.reload")) {
                    configManager.reloadConfig();
                    plugin.getAutoMessages().reload();
                    plugin.getChatManager().getChannelManager().reloadChannels();
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("reload-success")));
                } else {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("no-permission")));
                }
                return true;
            } else if (args[0].equalsIgnoreCase("spy")) {
                if (!(sender instanceof Player)) {
                    return true;
                }
                
                Player player = (Player) sender;
                if (!player.hasPermission(configManager.getSpyPermission())) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("spy.no-permission")));
                    return true;
                }
                
                if (args.length < 2) {
                    spyFunction.toggleSpy(player);
                } else if (args[1].equalsIgnoreCase("on")) {
                    spyFunction.enableSpy(player);
                } else if (args[1].equalsIgnoreCase("off")) {
                    spyFunction.disableSpy(player);
                } else {
                }
                return true;
            } else if (args[0].equalsIgnoreCase("channel")) {
                if (!(sender instanceof Player)) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("channel.players-only")));
                    return true;
                }
                
                Player player = (Player) sender;
                
                if (args.length < 2) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("channel.usage")));
                    return true;
                }
                
                String channelId = args[1];
                if (channelId.equalsIgnoreCase("default")) {
                    plugin.getChatManager().getChannelManager().setPlayerChannel(player.getUniqueId(), "default");
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("channel.switched-default")));
                    return true;
                }
                
                if (!plugin.getChatManager().getChannelManager().hasChannelPermission(player, channelId)) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("channel.no-permission")));
                    return true;
                }
                
                plugin.getChatManager().getChannelManager().setPlayerChannel(player.getUniqueId(), channelId);
                String message = configManager.getMessage("channel.switched").replace("{channel}", channelId);
                sender.sendMessage(HexUtils.translateAlternateColorCodes(message));
                return true;
            } else if (args[0].equalsIgnoreCase("clear")) {
                if (!sender.hasPermission("fcchat.clear")) {
                    sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("no-permission")));
                    return true;
                }
                
                for (int i = 0; i < 100; i++) {
                    Bukkit.broadcastMessage("");
                }
                String clearMessage = configManager.getMessage("clear-chat").replace("{player}", sender.getName());
                Bukkit.broadcastMessage(HexUtils.translateAlternateColorCodes(clearMessage));
                return true;
            } else {
                sender.sendMessage(HexUtils.translateAlternateColorCodes(configManager.getMessage("unknown-command")));
                return true;
            }
        }
        return false;
    }
} 