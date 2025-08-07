package fc.plugins.fcchat.listeners;

import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {
    private final ConfigManager configManager;

    public JoinLeaveListener(ConfigManager configManager) {
        this.configManager = configManager;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        
        if (configManager.isJoinLeaveMessagesEnabled()) {
            String message = configManager.getJoinMessage();
            if (message != null && !message.equalsIgnoreCase("null")) {
                message = message.replace("%player_name%", player.getName());
                
                PlaceholderAPIIntegration placeholderAPI = configManager.getPlaceholderAPI();
                if (placeholderAPI.isEnabled()) {
                    message = placeholderAPI.setPlaceholders(player, message);
                }
                
                message = HexUtils.translateAlternateColorCodes(message);
                event.setJoinMessage(message);
                
                String soundName = configManager.getJoinSound();
                if (soundName != null && !soundName.equalsIgnoreCase("null")) {
                    try {
                        Sound sound = Sound.valueOf(soundName.toUpperCase());
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                        }
                    } catch (IllegalArgumentException e) {
                    }
                }
            } else {
                event.setJoinMessage(null);
            }
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        
        if (configManager.isJoinLeaveMessagesEnabled()) {
            String message = configManager.getLeaveMessage();
            if (message != null && !message.equalsIgnoreCase("null")) {
                message = message.replace("%player_name%", player.getName());
                
                PlaceholderAPIIntegration placeholderAPI = configManager.getPlaceholderAPI();
                if (placeholderAPI.isEnabled()) {
                    message = placeholderAPI.setPlaceholders(player, message);
                }
                
                message = HexUtils.translateAlternateColorCodes(message);
                event.setQuitMessage(message);
                
                String soundName = configManager.getLeaveSound();
                if (soundName != null && !soundName.equalsIgnoreCase("null")) {
                    try {
                        Sound sound = Sound.valueOf(soundName.toUpperCase());
                        for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                            if (!onlinePlayer.equals(player)) {
                                onlinePlayer.playSound(onlinePlayer.getLocation(), sound, 1.0f, 1.0f);
                            }
                        }
                    } catch (IllegalArgumentException e) {
                    }
                }
            } else {
                event.setQuitMessage(null);
            }
        }
    }
}