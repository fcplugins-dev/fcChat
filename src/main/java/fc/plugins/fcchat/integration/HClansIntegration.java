package fc.plugins.fcchat.integration;

import java.util.Optional;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import ru.he3hauka.hclans.Main;
import ru.he3hauka.hclans.api.Clan;
import ru.he3hauka.hclans.api.ClanApi;

public class HClansIntegration {

    public boolean isEnabled() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("hClans");
            return plugin != null
                    && Bukkit.getPluginManager().isPluginEnabled("hClans")
                    && plugin instanceof Main
                    && ((Main) plugin).clanManager != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean hasClan(Player player) {
        return this.getClan(player) != null;
    }

    public String getClanName(Player player) {
        Clan clan = this.getClan(player);
        return clan == null ? null : clan.name();
    }

    public boolean isInClan(Player player, String clanName) {
        if (player == null || clanName == null) {
            return false;
        }
        Clan clan = this.getClan(player);
        return clan != null && clan.name().equalsIgnoreCase(clanName);
    }

    private Clan getClan(Player player) {
        if (!this.isEnabled() || player == null) {
            return null;
        }
        try {
            ClanApi clanApi = this.getClanApi();
            if (clanApi == null) {
                return null;
            }
            Optional<Clan> clan = clanApi.getClanByPlayer(player.getUniqueId());
            if (clan.isPresent() && clan.get().isMember(player.getUniqueId())) {
                return clan.get();
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private ClanApi getClanApi() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("hClans");
            return plugin instanceof Main ? ((Main) plugin).clanManager : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}