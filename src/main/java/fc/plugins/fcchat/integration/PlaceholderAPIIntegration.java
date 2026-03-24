package fc.plugins.fcchat.integration;

import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;

public class PlaceholderAPIIntegration {
    private boolean isEnabled = false;

    public PlaceholderAPIIntegration() {
        this.setupPlaceholderAPI();
    }

    private void setupPlaceholderAPI() {
        try {
            Class.forName("me.clip.placeholderapi.PlaceholderAPI");
            this.isEnabled = true;
        }
        catch (ClassNotFoundException e) {
            this.isEnabled = false;
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public String setPlaceholders(Player player, String text) {
        if (!this.isEnabled) {
            return text;
        }
        try {
            return PlaceholderAPI.setPlaceholders(player, text);
        } catch (Exception | NoSuchFieldError | NoSuchMethodError e) {
            return text;
        }
    }
}
