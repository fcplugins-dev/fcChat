package fc.plugins.fcchat.integration;

import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.NodeType;
import net.luckperms.api.node.types.PrefixNode;
import net.luckperms.api.node.types.SuffixNode;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;

public class LuckPermsIntegration {
    private LuckPerms luckPerms;
    private boolean isEnabled = false;

    public LuckPermsIntegration() {
        setupLuckPerms();
    }

    private void setupLuckPerms() {
        if (Bukkit.getPluginManager().getPlugin("LuckPerms") == null) {
            return;
        }

        RegisteredServiceProvider<LuckPerms> provider = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider != null) {
            luckPerms = provider.getProvider();
            isEnabled = true;
        }
    }

    public boolean isEnabled() {
        return isEnabled;
    }

    public String getPrefix(Player player) {
        if (!isEnabled) {
            return "";
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return "";
        }

        return user.getCachedData().getMetaData().getPrefix();
    }

    public String getSuffix(Player player) {
        if (!isEnabled) {
            return "";
        }

        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) {
            return "";
        }

        return user.getCachedData().getMetaData().getSuffix();
    }

    public String getPrefix(Player player, String defaultValue) {
        String prefix = getPrefix(player);
        return prefix != null ? prefix : defaultValue;
    }

    public String getSuffix(Player player, String defaultValue) {
        String suffix = getSuffix(player);
        return suffix != null ? suffix : defaultValue;
    }
} 