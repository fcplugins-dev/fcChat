package fc.plugins.fcchat.integration;

import fc.plugins.fcchat.FcChat;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import github.scarsz.discordsrv.util.DiscordUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

public class DiscordSrvIntegration {
    private final FcChat plugin;
    private final LuckPermsIntegration luckPermsIntegration;

    public DiscordSrvIntegration(FcChat plugin) {
        this.plugin = plugin;
        this.luckPermsIntegration = new LuckPermsIntegration();
    }

    public void sendGlobalChat(Player player, String message) {
        if (!this.plugin.getConfigManager().isDiscordSrvEnabled()) {
            return;
        }
        if (player == null || message == null || message.trim().isEmpty()) {
            return;
        }

        Plugin discordSrvPlugin = Bukkit.getPluginManager().getPlugin("DiscordSRV");
        if (discordSrvPlugin == null || !discordSrvPlugin.isEnabled()) {
            return;
        }

        if (!DiscordSRV.isReady) {
            this.plugin.getCompatScheduler().runGlobalLater(40L, () -> {
                if (DiscordSRV.isReady) {
                    this.sendNow(player, message);
                }
            });
            return;
        }

        this.sendNow(player, message);
    }

    private void sendNow(Player player, String message) {
        TextChannel channel = DiscordSRV.getPlugin().getOptionalTextChannel("global");
        if (channel == null) {
            return;
        }

        String prefix = this.luckPermsIntegration.getPrefix(player, "");
        String displayName = (prefix != null && !prefix.isEmpty()) ? (prefix + " " + player.getName()) : player.getName();
        DiscordUtil.sendMessage(channel, "**" + displayName + "**: " + message);
    }
}

