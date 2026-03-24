package fc.plugins.fcchat.manager;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CooldownManager {
    private final ConfigManager configManager;
    private final Map<UUID, Long> msgCooldowns;
    private final Map<UUID, Long> replyCooldowns;
    private final Map<UUID, Long> broadcastCooldowns;

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.msgCooldowns = new HashMap<>();
        this.replyCooldowns = new HashMap<>();
        this.broadcastCooldowns = new HashMap<>();
    }

    public boolean isOnCooldown(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long cooldownTime = 0;
        Map<UUID, Long> cooldownMap = null;

        switch (command.toLowerCase()) {
            case "msg":
                cooldownTime = this.configManager.getPrivateMessageConfig().getInt("private-messages.cooldown") * 1000L;
                cooldownMap = this.msgCooldowns;
                break;
            case "reply":
            case "r":
                cooldownTime = this.configManager.getPrivateMessageConfig().getInt("reply.cooldown") * 1000L;
                cooldownMap = this.replyCooldowns;
                break;
            case "broadcast":
            case "bc":
                cooldownTime = this.configManager.getPrivateMessageConfig().getInt("broadcast.cooldown") * 1000L;
                cooldownMap = this.broadcastCooldowns;
                break;
        }

        if (cooldownMap == null || cooldownTime <= 0) {
            return false;
        }

        Long lastUsed = cooldownMap.get(playerId);
        if (lastUsed == null) {
            return false;
        }

        long timeLeft = (lastUsed + cooldownTime) - currentTime;
        return timeLeft > 0;
    }

    public long getCooldownTimeLeft(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long cooldownTime = 0;
        Map<UUID, Long> cooldownMap = null;

        switch (command.toLowerCase()) {
            case "msg":
                cooldownTime = this.configManager.getPrivateMessageConfig().getInt("private-messages.cooldown") * 1000L;
                cooldownMap = this.msgCooldowns;
                break;
            case "reply":
            case "r":
                cooldownTime = this.configManager.getPrivateMessageConfig().getInt("reply.cooldown") * 1000L;
                cooldownMap = this.replyCooldowns;
                break;
            case "broadcast":
            case "bc":
                cooldownTime = this.configManager.getPrivateMessageConfig().getInt("broadcast.cooldown") * 1000L;
                cooldownMap = this.broadcastCooldowns;
                break;
        }

        if (cooldownMap == null || cooldownTime <= 0) {
            return 0;
        }

        Long lastUsed = cooldownMap.get(playerId);
        if (lastUsed == null) {
            return 0;
        }

        long timeLeft = (lastUsed + cooldownTime) - currentTime;
        return Math.max(0, timeLeft);
    }

    public void setCooldown(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();

        switch (command.toLowerCase()) {
            case "msg":
                this.msgCooldowns.put(playerId, currentTime);
                break;
            case "reply":
            case "r":
                this.replyCooldowns.put(playerId, currentTime);
                break;
            case "broadcast":
            case "bc":
                this.broadcastCooldowns.put(playerId, currentTime);
                break;
        }
    }

    public void sendCooldownMessage(Player player, String command) {
        long timeLeft = getCooldownTimeLeft(player, command);
        long secondsLeft = (timeLeft + 999) / 1000;

        String message = "";
        switch (command.toLowerCase()) {
            case "msg":
                message = this.configManager.getPrivateMessageConfig().getString("private-messages.cooldown-message");
                break;
            case "reply":
            case "r":
                message = this.configManager.getPrivateMessageConfig().getString("reply.cooldown-message");
                break;
            case "broadcast":
            case "bc":
                message = this.configManager.getPrivateMessageConfig().getString("broadcast.cooldown-message");
                break;
        }

        if (!message.isEmpty()) {
            message = message.replace("{time}", String.valueOf(secondsLeft));
            player.sendMessage(HexUtils.translateAlternateColorCodes(message));
        }
    }
}
