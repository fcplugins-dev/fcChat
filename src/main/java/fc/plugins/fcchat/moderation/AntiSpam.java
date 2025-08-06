package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.data.PlayerTimeManager;
import org.bukkit.entity.Player;
import org.bukkit.ChatColor;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class AntiSpam {
    private final ConfigManager configManager;
    private final PlayerTimeManager playerTimeManager;
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<>();

    public AntiSpam(ConfigManager configManager, PlayerTimeManager playerTimeManager) {
        this.configManager = configManager;
        this.playerTimeManager = playerTimeManager;
    }

    public boolean isSpam(Player player) {
        if (!configManager.isAntiSpamEnabled()) {
            return false;
        }

        String bypassPermission = configManager.getAntiSpamBypassPermission();
        if (bypassPermission != null && player.hasPermission(bypassPermission)) {
            return false;
        }

        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(playerId, 0L);
        double cooldown = configManager.getAntiSpamCooldown() * 1000;

        if (currentTime - lastTime < cooldown) {
            return true;
        }

        lastMessageTime.put(playerId, currentTime);
        return false;
    }

    public boolean isNewPlayerBlocked(Player player) {
        if (!configManager.isNewPlayerChatEnabled()) {
            return false;
        }

        String bypassPermission = configManager.getNewPlayerBypassPermission();
        if (bypassPermission != null && player.hasPermission(bypassPermission)) {
            return false;
        }

        long totalPlaytime = playerTimeManager.getTotalPlaytimeWithSession(player);
        long blockTime = configManager.getNewPlayerBlockTime() * 1000;

        return totalPlaytime < blockTime;
    }

    public String getAntiSpamMessage(double remainingTime) {
        String message = configManager.getAntiSpamMessage();
        return ChatColor.translateAlternateColorCodes('&', message.replace("{time}", String.format("%.0f", remainingTime)));
    }

    public String getNewPlayerMessage(double remainingTime) {
        String message = configManager.getNewPlayerMessage();
        return ChatColor.translateAlternateColorCodes('&', message.replace("{time}", String.format("%.0f", remainingTime)));
    }

    public double getRemainingSpamTime(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTime = lastMessageTime.getOrDefault(playerId, 0L);
        double cooldown = configManager.getAntiSpamCooldown() * 1000;
        double remaining = (cooldown - (currentTime - lastTime)) / 1000.0;
        return Math.max(0, remaining);
    }

    public double getRemainingNewPlayerTime(Player player) {
        long totalPlaytime = playerTimeManager.getTotalPlaytimeWithSession(player);
        long blockTime = configManager.getNewPlayerBlockTime() * 1000;
        double remaining = (blockTime - totalPlaytime) / 1000.0;
        return Math.max(0, remaining);
    }
} 