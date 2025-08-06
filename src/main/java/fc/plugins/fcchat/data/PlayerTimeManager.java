package fc.plugins.fcchat.data;

import fc.plugins.fcchat.FcChat;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerTimeManager {
    private final FcChat plugin;
    private final File playtimeFile;
    private FileConfiguration playtimeConfig;
    private final Map<UUID, Long> playerJoinTimes = new HashMap<>();

    public PlayerTimeManager(FcChat plugin) {
        this.plugin = plugin;
        this.playtimeFile = new File(plugin.getDataFolder(), "data/playtime.yml");
        loadPlaytimeData();
        loadOnlinePlayersOnStartup();
    }

    private void loadPlaytimeData() {
        if (!playtimeFile.exists()) {
            playtimeFile.getParentFile().mkdirs();
            try {
                playtimeFile.createNewFile();
            } catch (IOException e) {
            }
        }
        playtimeConfig = YamlConfiguration.loadConfiguration(playtimeFile);
    }

    private void loadOnlinePlayersOnStartup() {
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                onPlayerJoin(player);
            }
        }, 1L);
    }

    public void onPlayerJoin(Player player) {
        UUID playerId = player.getUniqueId();
        playerJoinTimes.put(playerId, System.currentTimeMillis());
    }

    public void onPlayerQuit(Player player) {
        UUID playerId = player.getUniqueId();
        Long joinTime = playerJoinTimes.remove(playerId);
        
        if (joinTime != null) {
            long currentTime = System.currentTimeMillis();
            long sessionTime = currentTime - joinTime;
            
            long totalTime = getTotalPlaytime(playerId) + sessionTime;
            saveTotalPlaytime(playerId, totalTime);
        }
    }

    public long getTotalPlaytime(UUID playerId) {
        return playtimeConfig.getLong("players." + playerId.toString() + ".total-time");
    }

    public long getSessionPlaytime(Player player) {
        UUID playerId = player.getUniqueId();
        Long joinTime = playerJoinTimes.get(playerId);
        
        if (joinTime == null) {
            return 0L;
        }
        
        return System.currentTimeMillis() - joinTime;
    }

    public long getTotalPlaytimeWithSession(Player player) {
        return getTotalPlaytime(player.getUniqueId()) + getSessionPlaytime(player);
    }

    private void saveTotalPlaytime(UUID playerId, long totalTime) {
        playtimeConfig.set("players." + playerId.toString() + ".total-time", totalTime);
        try {
            playtimeConfig.save(playtimeFile);
        } catch (IOException e) {
        }
    }

    public void saveAllData() {
        for (Map.Entry<UUID, Long> entry : playerJoinTimes.entrySet()) {
            UUID playerId = entry.getKey();
            Long joinTime = entry.getValue();
            
            long currentTime = System.currentTimeMillis();
            long sessionTime = currentTime - joinTime;
            long totalTime = getTotalPlaytime(playerId) + sessionTime;
            
            saveTotalPlaytime(playerId, totalTime);
        }
    }

    public long getPlayerTime(String playerName) {
        Player player = plugin.getServer().getPlayer(playerName);
        if (player != null) {
            return getTotalPlaytimeWithSession(player);
        }

        for (Object uuidString : playtimeConfig.getConfigurationSection("players") != null ?
             playtimeConfig.getConfigurationSection("players").getKeys(false) : new java.util.ArrayList<>()) {
            try {
                UUID uuid = UUID.fromString((String) uuidString);
                Player offlinePlayer = plugin.getServer().getOfflinePlayer(uuid).getPlayer();
                if (offlinePlayer.getName() != null && offlinePlayer.getName().equals(playerName)) {
                    return getTotalPlaytime(uuid);
                }
            } catch (IllegalArgumentException e) {
            }
        }
        
        return 0L;
    }
} 