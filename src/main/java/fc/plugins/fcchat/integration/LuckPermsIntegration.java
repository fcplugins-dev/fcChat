package fc.plugins.fcchat.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsIntegration {
    private LuckPerms luckPerms;
    private boolean isEnabled = false;

    public LuckPermsIntegration() {
        this.setupLuckPerms();
    }

    private void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }
        RegisteredServiceProvider provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            this.luckPerms = (LuckPerms)provider.getProvider();
            this.isEnabled = true;
        }
    }

    public boolean isEnabled() {
        return this.isEnabled;
    }

    public String getPrefix(Player player) {
        if (!this.isEnabled) {
            return "";
        }
        User user = this.luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return "";
        }
        return user.getCachedData().getMetaData().getPrefix();
    }

    public String getSuffix(Player player) {
        if (!this.isEnabled) {
            return "";
        }
        User user = this.luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return "";
        }
        return user.getCachedData().getMetaData().getSuffix();
    }

    public String getPrefix(Player player, String defaultValue) {
        String prefix = this.getPrefix(player);
        return prefix != null ? prefix : defaultValue;
    }

    public String getSuffix(Player player, String defaultValue) {
        String suffix = this.getSuffix(player);
        return suffix != null ? suffix : defaultValue;
    }

    public String getPrimaryGroup(Player player) {
        if (!this.isEnabled) {
            return "default";
        }
        User user = this.luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return "default";
        }
        return user.getPrimaryGroup();
    }

    public String getPrimaryGroup(Player player, String defaultValue) {
        String group = this.getPrimaryGroup(player);
        return group != null ? group : defaultValue;
    }
}
