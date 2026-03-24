package fc.plugins.fcchat.chat.listeners;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener
        implements Listener {
    private final ConfigManager configManager;

    public JoinLeaveListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (this.configManager.isJoinLeaveMessagesEnabled()) {
            String message = this.configManager.getJoinMessage();
            if (message != null && !message.equalsIgnoreCase("null")) {
                message = message.replace("%player_name%", player.getName());
                message = message.replace("{player}", player.getName());
                PlaceholderAPIIntegration placeholderAPI = this.configManager.getPlaceholderAPI();
                if (placeholderAPI != null && placeholderAPI.isEnabled()) {
                    try {
                        message = placeholderAPI.setPlaceholders(player, message);
                    } catch (Exception e) {
                    }
                }
                message = HexUtils.translateAlternateColorCodes(message);
                event.setJoinMessage(message);
                String soundName = this.configManager.getJoinSound();
                if (soundName != null && !soundName.equalsIgnoreCase("null")) {
                    try {
                        NamespacedKey soundKey = NamespacedKey.fromString(soundName.toLowerCase());
                        if (soundKey != null) {
                            Sound sound = Registry.SOUNDS.get(soundKey);
                            if (sound != null) {
                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                                }
                            }
                        }
                    }
                    catch (IllegalArgumentException illegalArgumentException) {}
                }
            } else {
                event.setJoinMessage(null);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        if (this.configManager.isJoinLeaveMessagesEnabled()) {
            String message = this.configManager.getLeaveMessage();
            if (message != null && !message.equalsIgnoreCase("null")) {
                message = message.replace("%player_name%", player.getName());
                message = message.replace("{player}", player.getName());
                PlaceholderAPIIntegration placeholderAPI = this.configManager.getPlaceholderAPI();
                if (placeholderAPI != null && placeholderAPI.isEnabled()) {
                    try {
                        message = placeholderAPI.setPlaceholders(player, message);
                    } catch (Exception e) {
                    }
                }
                message = HexUtils.translateAlternateColorCodes(message);
                event.setQuitMessage(message);
                String soundName = this.configManager.getLeaveSound();
                if (soundName != null && !soundName.equalsIgnoreCase("null")) {
                    try {
                        NamespacedKey soundKey = NamespacedKey.fromString(soundName.toLowerCase());
                        if (soundKey != null) {
                            Sound sound = Registry.SOUNDS.get(soundKey);
                            if (sound != null) {
                                for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                                    if (onlinePlayer.equals(player)) continue;
                                    onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                                }
                            }
                        }
                    }
                    catch (IllegalArgumentException illegalArgumentException) {}
                }
            } else {
                event.setQuitMessage(null);
            }
        }
    }
}
