package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.data.PlayerTimeManager;
import fc.plugins.fcchat.utils.HexUtils;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Player;

public class AntiSpam {
    private final ConfigManager configManager;
    private final PlayerTimeManager playerTimeManager;
    private final Map<UUID, Long> lastMessageTime = new ConcurrentHashMap<UUID, Long>();

    public AntiSpam(ConfigManager configManager, PlayerTimeManager playerTimeManager) {
        this.configManager = configManager;
        this.playerTimeManager = playerTimeManager;
    }

    public boolean isSpam(Player player) {
        double cooldown;
        long lastTime;
        if (!this.configManager.isAntiSpamEnabled()) {
            return false;
        }
        String bypassPermission = this.configManager.getAntiSpamBypassPermission();
        if (bypassPermission != null && player.hasPermission(bypassPermission)) {
            return false;
        }
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        if ((double)(currentTime - (lastTime = this.lastMessageTime.getOrDefault(playerId, 0L).longValue())) < (cooldown = this.configManager.getAntiSpamCooldown() * 1000.0)) {
            return true;
        }
        this.lastMessageTime.put(playerId, currentTime);
        return false;
    }

    public boolean isNewPlayerBlocked(Player player) {
        if (!this.configManager.isNewPlayerChatEnabled()) {
            return false;
        }
        String bypassPermission = this.configManager.getNewPlayerBypassPermission();
        if (bypassPermission != null && player.hasPermission(bypassPermission)) {
            return false;
        }
        long worldPlaytime = this.playerTimeManager.getTimeSinceFirstJoin(player);
        long blockTime = (long)(this.configManager.getNewPlayerBlockTime() * 1000);
        return worldPlaytime < blockTime;
    }

    public boolean hasTooManyCaps(String message, Player player) {
        if (!this.configManager.isAntiCapsEnabled()) {
            return false;
        }
        String bypassPermission = this.configManager.getAntiCapsBypassPermission();
        if (bypassPermission != null && player.hasPermission(bypassPermission)) {
            return false;
        }
        int capsCount = 0;
        int letterCount = 0;
        for (char c : message.toCharArray()) {
            if (Character.isLetter(c)) {
                letterCount++;
                if (Character.isUpperCase(c)) {
                    capsCount++;
                }
            }
        }
        if (letterCount == 0) {
            return false;
        }
        int capsPercent = (capsCount * 100) / letterCount;
        int maxPercent = this.configManager.getAntiCapsPercent();
        return capsPercent > maxPercent;
    }

    public String processCaps(String message, Player player) {
        if (!hasTooManyCaps(message, player)) {
            return message;
        }
        String mode = this.configManager.getAntiCapsMode();
        if ("lowercase".equalsIgnoreCase(mode)) {
            return message.toLowerCase();
        }
        return message;
    }

    public String getAntiCapsMessage() {
        String message = this.configManager.getAntiCapsMessage();
        return HexUtils.translateAlternateColorCodes(message);
    }

    public String getAntiSpamMessage(double remainingTime) {
        String message = this.configManager.getAntiSpamMessage();
        return HexUtils.translateAlternateColorCodes(message.replace("{time}", String.format("%.0f", remainingTime)));
    }

    public String getNewPlayerMessage(double remainingTime) {
        String message = this.configManager.getNewPlayerMessage();
        return HexUtils.translateAlternateColorCodes(message.replace("{time}", String.format("%.0f", remainingTime)));
    }

    public double getRemainingSpamTime(Player player) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long lastTime = this.lastMessageTime.getOrDefault(playerId, 0L);
        double cooldown = this.configManager.getAntiSpamCooldown() * 1000.0;
        double remaining = (cooldown - (double)(currentTime - lastTime)) / 1000.0;
        return Math.max(0.0, remaining);
    }

    public double getRemainingNewPlayerTime(Player player) {
        long worldPlaytime = this.playerTimeManager.getTimeSinceFirstJoin(player);
        long blockTime = this.configManager.getNewPlayerBlockTime() * 1000;
        double remaining = (double)(blockTime - worldPlaytime) / 1000.0;
        return Math.max(0.0, remaining);
    }
}
