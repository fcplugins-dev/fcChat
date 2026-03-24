package fc.plugins.fcchat.chat.listeners;

import fc.plugins.fcchat.FcChat;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class UpdateListener
        implements Listener {
    private final FcChat plugin;

    public UpdateListener(FcChat plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (player.hasPermission("fcchat.update")) {
            this.plugin.getUpdater().sendUpdateMessageToPlayer(player);
        }
    }
}
