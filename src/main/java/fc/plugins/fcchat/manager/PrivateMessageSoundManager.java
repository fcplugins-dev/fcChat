package fc.plugins.fcchat.manager;

import fc.plugins.fcchat.manager.config.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PrivateMessageSoundManager {
    private final ConfigManager configManager;

    public PrivateMessageSoundManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void playPrivateMessageSound(Player player, String type) {
        boolean enabled = this.configManager.getPrivateMessageConfig().getBoolean("private-messages.sounds." + type + ".enabled");
        if (!enabled) return;

        String soundName = this.configManager.getPrivateMessageConfig().getString("private-messages.sounds." + type + ".sound");
        float volume = (float) this.configManager.getPrivateMessageConfig().getDouble("private-messages.sounds." + type + ".volume");
        float pitch = (float) this.configManager.getPrivateMessageConfig().getDouble("private-messages.sounds." + type + ".pitch");

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Invalid sound name, ignore
        }
    }

    public void playReplySound(Player player, String type) {
        boolean enabled = this.configManager.getPrivateMessageConfig().getBoolean("reply.sounds." + type + ".enabled");
        if (!enabled) return;

        String soundName = this.configManager.getPrivateMessageConfig().getString("reply.sounds." + type + ".sound");
        float volume = (float) this.configManager.getPrivateMessageConfig().getDouble("reply.sounds." + type + ".volume");
        float pitch = (float) this.configManager.getPrivateMessageConfig().getDouble("reply.sounds." + type + ".pitch");

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Invalid sound name, ignore
        }
    }

    public void playBroadcastSound(Player player) {
        boolean enabled = this.configManager.getPrivateMessageConfig().getBoolean("broadcast.sounds.enabled");
        if (!enabled) return;

        String soundName = this.configManager.getPrivateMessageConfig().getString("broadcast.sounds.sound");
        float volume = (float) this.configManager.getPrivateMessageConfig().getDouble("broadcast.sounds.volume");
        float pitch = (float) this.configManager.getPrivateMessageConfig().getDouble("broadcast.sounds.pitch");

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
            // Invalid sound name, ignore
        }
    }
}
