package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.config.ConfigManager;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class SoundManager {
    private final ConfigManager configManager;

    public SoundManager(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public void playMessageSound(Player player) {
        if (!configManager.isMessageSoundEnabled()) {
            return;
        }

        String soundName = configManager.getMessageSound();
        float volume = configManager.getMessageSoundVolume();
        float pitch = configManager.getMessageSoundPitch();

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
        }
    }

    public void playPingSound(Player player) {
        if (!configManager.isPingSoundEnabled()) {
            return;
        }

        String soundName = configManager.getPingSound();
        float volume = configManager.getPingSoundVolume();
        float pitch = configManager.getPingSoundPitch();

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
        }
    }

    public void playEveryonePingSound(Player player) {
        if (!configManager.isEveryonePingSoundEnabled()) {
            return;
        }

        String soundName = configManager.getEveryonePingSound();
        float volume = configManager.getEveryonePingSoundVolume();
        float pitch = configManager.getEveryonePingSoundPitch();

        try {
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
        }
    }
}
