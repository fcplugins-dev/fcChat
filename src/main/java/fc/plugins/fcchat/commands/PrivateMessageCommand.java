package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class PrivateMessageCommand implements CommandExecutor {
    private final FcChat plugin;
    private final ConfigManager configManager;

    public PrivateMessageCommand(FcChat plugin, ConfigManager configManager) {
        this.plugin = plugin;
        this.configManager = configManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("players-only")));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("fcchat.msg")) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.no-permission")));
            return true;
        }

        if (args.length < 2) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.no-message")));
            return true;
        }

        if (!player.hasPermission("fcchat.bypass") && this.plugin.getCooldownManager().isOnCooldown(player, "msg")) {
            this.plugin.getCooldownManager().sendCooldownMessage(player, "msg");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.player-not-found").replace("{player}", targetName)));
            return true;
        }

        if (target.equals(player)) {
            player.sendMessage(HexUtils.translateAlternateColorCodes(this.configManager.getMessage("private-messages.cannot-message-yourself")));
            return true;
        }

        StringBuilder messageBuilder = new StringBuilder();
        for (int i = 1; i < args.length; i++) {
            messageBuilder.append(args[i]);
            if (i < args.length - 1) {
                messageBuilder.append(" ");
            }
        }
        String message = messageBuilder.toString();

        boolean sent = this.plugin.getApi().sendPrivateMessage(player, target, message);
        if (!sent) {
            return true;
        }

        if (!player.hasPermission("fcchat.bypass")) {
            this.plugin.getCooldownManager().setCooldown(player, "msg");
        }
        return true;
    }
}
