package fc.plugins.fcchat.integration;

import java.util.Map;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import phrase.towerclans.TowerClansAPI;
import phrase.towerclans.manager.ClanManager;
import phrase.towerclans.model.Clan;
import phrase.towerclans.model.ClanEntity;

public class TowerClansIntegration {

    public boolean isEnabled() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("TowerClans");
            return plugin != null
                    && Bukkit.getPluginManager().isPluginEnabled("TowerClans")
                    && plugin instanceof TowerClansAPI
                    && getClanManager() != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    public boolean hasClan(Player player) {
        return this.getClan(player) != null;
    }

    public String getClanName(Player player) {
        Clan clan = this.getClan(player);
        return clan == null ? null : clan.getName();
    }

    public boolean isInClan(Player player, String clanName) {
        if (player == null || clanName == null) {
            return false;
        }
        Clan clan = this.getClan(player);
        return clan != null && clan.getName().equalsIgnoreCase(clanName);
    }

    private Clan getClan(Player player) {
        if (!this.isEnabled() || player == null) {
            return null;
        }
        try {
            ClanManager<Clan> clanManager = this.getClanManager();
            if (clanManager == null) {
                return null;
            }
            for (Clan clan : clanManager.getClans().values()) {
                if (this.clanHasMember(clan, player)) {
                    return clan;
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private boolean clanHasMember(Clan clan, Player player) {
        try {
            Map<ClanEntity, String> members = clan.getMembers();
            for (ClanEntity member : members.keySet()) {
                if (player.getUniqueId().equals(member.getPlayerUUID())) {
                    return true;
                }
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    private ClanManager<Clan> getClanManager() {
        try {
            Plugin plugin = Bukkit.getPluginManager().getPlugin("TowerClans");
            return plugin instanceof TowerClansAPI ? ((TowerClansAPI) plugin).getClanManager() : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}