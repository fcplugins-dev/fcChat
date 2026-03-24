package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChannelCommand implements CommandExecutor {
    private final FcChat plugin;
    private final ConfigManager configManager;

    public ChannelCommand(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.players-only")));
            return true;
        }
        Player player = (Player) sender;
        if (args.length == 0) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("channel.usage")));
            return true;
        }
        String channelId = args[0];
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
}
