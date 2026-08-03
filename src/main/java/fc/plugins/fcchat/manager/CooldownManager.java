
package fc.plugins.fcchat.manager;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public class CooldownManager {
    private final ConfigManager configManager;
    private final Map<UUID, Long> msgCooldowns;
    private final Map<UUID, Long> replyCooldowns;
    private final Map<UUID, Long> broadcastCooldowns;

    public CooldownManager(ConfigManager configManager) {
        this.configManager = configManager;
        this.msgCooldowns = new HashMap<UUID, Long>();
        this.replyCooldowns = new HashMap<UUID, Long>();
        this.broadcastCooldowns = new HashMap<UUID, Long>();
    }

    public boolean isOnCooldown(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long cooldownTime = 0L;
        Map<UUID, Long> cooldownMap = null;
        switch (command.toLowerCase()) {
            case "msg": {
                cooldownTime = (long)this.configManager.getPrivateMessageConfig().getInt("private-messages.cooldown") * 1000L;
                cooldownMap = this.msgCooldowns;
                break;
            }
            case "r": 
            case "reply": {
                cooldownTime = (long)this.configManager.getPrivateMessageConfig().getInt("reply.cooldown") * 1000L;
                cooldownMap = this.replyCooldowns;
                break;
            }
            case "broadcast": 
            case "bc": {
                cooldownTime = (long)this.configManager.getPrivateMessageConfig().getInt("broadcast.cooldown") * 1000L;
                cooldownMap = this.broadcastCooldowns;
            }
        }
        if (cooldownMap == null || cooldownTime <= 0L) {
            return false;
        }
        Long lastUsed = (Long)cooldownMap.get(playerId);
        if (lastUsed == null) {
            return false;
        }
        long timeLeft = lastUsed + cooldownTime - currentTime;
        return timeLeft > 0L;
    }

    public long getCooldownTimeLeft(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        long cooldownTime = 0L;
        Map<UUID, Long> cooldownMap = null;
        switch (command.toLowerCase()) {
            case "msg": {
                cooldownTime = (long)this.configManager.getPrivateMessageConfig().getInt("private-messages.cooldown") * 1000L;
                cooldownMap = this.msgCooldowns;
                break;
            }
            case "r": 
            case "reply": {
                cooldownTime = (long)this.configManager.getPrivateMessageConfig().getInt("reply.cooldown") * 1000L;
                cooldownMap = this.replyCooldowns;
                break;
            }
            case "broadcast": 
            case "bc": {
                cooldownTime = (long)this.configManager.getPrivateMessageConfig().getInt("broadcast.cooldown") * 1000L;
                cooldownMap = this.broadcastCooldowns;
            }
        }
        if (cooldownMap == null || cooldownTime <= 0L) {
            return 0L;
        }
        Long lastUsed = (Long)cooldownMap.get(playerId);
        if (lastUsed == null) {
            return 0L;
        }
        long timeLeft = lastUsed + cooldownTime - currentTime;
        return Math.max(0L, timeLeft);
    }

    public void setCooldown(Player player, String command) {
        UUID playerId = player.getUniqueId();
        long currentTime = System.currentTimeMillis();
        switch (command.toLowerCase()) {
            case "msg": {
                this.msgCooldowns.put(playerId, currentTime);
                break;
            }
            case "r": 
            case "reply": {
                this.replyCooldowns.put(playerId, currentTime);
                break;
            }
            case "broadcast": 
            case "bc": {
                this.broadcastCooldowns.put(playerId, currentTime);
            }
        }
    }

    public void sendCooldownMessage(Player player, String command) {
        long timeLeft = this.getCooldownTimeLeft(player, command);
        long secondsLeft = (timeLeft + 999L) / 1000L;
        String message = "";
        switch (command.toLowerCase()) {
            case "msg": {
                message = this.configManager.getPrivateMessageConfig().getString("private-messages.cooldown-message");
                break;
            }
            case "r": 
            case "reply": {
                message = this.configManager.getPrivateMessageConfig().getString("reply.cooldown-message");
                break;
            }
            case "broadcast": 
            case "bc": {
                message = this.configManager.getPrivateMessageConfig().getString("broadcast.cooldown-message");
            }
        }
        if (!message.isEmpty()) {
            message = message.replace("{time}", String.valueOf(secondsLeft));
            player.sendMessage(HexUtils.translateAlternateColorCodes(message));
        }
    }
}

