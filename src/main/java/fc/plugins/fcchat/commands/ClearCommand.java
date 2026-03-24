package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ClearCommand implements CommandExecutor {
    private final FcChat plugin;
    private final ConfigManager configManager;

    public ClearCommand(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("fcchat.clear")) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("no-permission")));
            return true;
        }
        this.plugin.getApi().clearChat(sender, true);
        return true;
    }
}
