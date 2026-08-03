
package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ReplyCommand
implements CommandExecutor {
    private final FcChat plugin;
    private final ConfigManager configManager;

    public ReplyCommand(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("players-only")));
            return true;
        }
        Player player = (Player)sender;
        if (!player.hasPermission("fcchat.reply")) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.no-permission")));
            return true;
        }
        if (args.length < 1) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.no-reply")));
            return true;
        }
        if (!player.hasPermission("fcchat.bypass") && this.plugin.getCooldownManager().isOnCooldown(player, "reply")) {
            this.plugin.getCooldownManager().sendCooldownMessage(player, "reply");
            return true;
        }
        UUID lastMessenger = this.plugin.getPrivateMessageManager().getLastMessenger(player.getUniqueId());
        if (lastMessenger == null) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.no-one-to-reply")));
            return true;
        }
        Player target = Bukkit.getPlayer((UUID)lastMessenger);
        if (target == null) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.player-offline").replace("{player}", "Unknown")));
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
        boolean sent = this.plugin.getApi().sendReplyMessage(player, target, message);
        if (!sent) {
            return true;
        }
        if (!player.hasPermission("fcchat.bypass")) {
            this.plugin.getCooldownManager().setCooldown(player, "reply");
        }
        return true;
    }
}

