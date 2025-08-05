package fc.plugins.fcchat.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderAPIIntegration {
    private boolean isEnabled = false;

    public PlaceholderAPIIntegration() {
        setupPlaceholderAPI();
    }

    private void setupPlaceholderAPI() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            isEnabled = true;
        } catch (ClassNotFoundException e) {
            isEnabled = false;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public String setPlaceholders(Player player, String text) {
        if (!isEnabled) {
            return text;
        }
        return PlaceholderAPI.setPlaceholders(player, text);
    }
} 