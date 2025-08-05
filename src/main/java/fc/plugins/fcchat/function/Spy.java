package fc.plugins.fcchat.function;

import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.entity.Player;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Spy {
    private final ConfigManager configManager;
    private final Set<UUID> spyPlayers;

    public Spy(ConfigManager configManager) {
        this.configManager = configManager;
        this.spyPlayers = new HashSet<>();
    }

    public boolean isSpying(Player player) {
        return spyPlayers.contains(player.getUniqueId());
    }

    public void enableSpy(Player player) {
        spyPlayers.add(player.getUniqueId());
        String message = configManager.getMessage("spy.enabled");
        player.sendMessage(HexUtils.translateAlternateColorCodes(message));
    }

    public void disableSpy(Player player) {
        spyPlayers.remove(player.getUniqueId());
        String message = configManager.getMessage("spy.disabled");
        player.sendMessage(HexUtils.translateAlternateColorCodes(message));
    }

    public void toggleSpy(Player player) {
        if (isSpying(player)) {
            disableSpy(player);
        } else {
            enableSpy(player);
        }
    }

    public Set<UUID> getSpyPlayers() {
        return new HashSet<>(spyPlayers);
    }

    public void sendSpyMessage(Player sender, String message, String formattedMessage) {
        String spyMessage = configManager.getSpyMessageFormat()
                .replace("{player}", sender.getName())
                .replace("{message}", message)
                .replace("{formatted}", formattedMessage);

        int spyRadius = configManager.getLocalChatRadius();
        
        for (UUID spyUUID : spyPlayers) {
            Player spyPlayer = org.bukkit.Bukkit.getPlayer(spyUUID);
            if (spyPlayer != null && spyPlayer.isOnline() && !spyPlayer.equals(sender)) {
                if (spyPlayer.getWorld().equals(sender.getWorld()) &&
                    spyPlayer.getLocation().distance(sender.getLocation()) > spyRadius) {
                    spyPlayer.sendMessage(HexUtils.translateAlternateColorCodes(spyMessage));
                }
            }
        }
    }
} 