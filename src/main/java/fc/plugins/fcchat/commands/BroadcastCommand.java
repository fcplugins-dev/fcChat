
package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class BroadcastCommand
implements CommandExecutor {
    private final FcChat plugin;
    private final ConfigManager configManager;

    public BroadcastCommand(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        Player player;
        if (!sender.hasPermission("fcchat.broadcast")) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("broadcast.no-permission")));
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("broadcast.no-message")));
            return true;
        }
        if (sender instanceof Player && !(player = (Player)sender).hasPermission("fcchat.bypass") && this.plugin.getCooldownManager().isOnCooldown(player, "broadcast")) {
            this.plugin.getCooldownManager().sendCooldownMessage(player, "broadcast");
            return true;
        }
        StringBuilder messageBuilder = new StringBuilder();
        String[] stringArray = args;
        int n = args.length;
        int n2 = 0;
        while (n2 < n) {
            String arg = stringArray[n2];
            messageBuilder.append(arg).append(" ");
            ++n2;
        }
        String message = messageBuilder.toString().trim();
        boolean sent = this.plugin.getApi().sendBroadcast(sender, message);
        if (!sent) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("broadcast.cancelled")));
            return true;
        }
        sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("broadcast.success")));
        if (sender instanceof Player && !((Player)sender).hasPermission("fcchat.bypass")) {
            this.plugin.getCooldownManager().setCooldown((Player)sender, "broadcast");
        }
        return true;
    }
}

