package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.manager.config.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
    private final ConfigManager configManager;

    public SoundManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void playMessageSound(Player player) {
        if (this.configManager.isMessageSoundEnabled()) {
            String soundName = this.configManager.getMessageSound();
            float volume = this.configManager.getMessageSoundVolume();
            float pitch = this.configManager.getMessageSoundPitch();

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException var6) {
            }

        }
    }

    public void playPingSound(Player player) {
        if (this.configManager.isPingSoundEnabled()) {
            String soundName = this.configManager.getPingSound();
            float volume = this.configManager.getPingSoundVolume();
            float pitch = this.configManager.getPingSoundPitch();

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException var6) {
            }

        }
    }

    public void playEveryonePingSound(Player player) {
        if (this.configManager.isEveryonePingSoundEnabled()) {
            String soundName = this.configManager.getEveryonePingSound();
            float volume = this.configManager.getEveryonePingSoundVolume();
            float pitch = this.configManager.getEveryonePingSoundPitch();

            try {
                Sound sound = Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, volume, pitch);
            } catch (IllegalArgumentException var6) {
            }

        }
    }
}