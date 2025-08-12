package fc.plugins.fcchat.holograms;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.utils.HexUtils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings("deprecation")
public class HologramsManager {
    private final FcChat plugin;
    private final Map<UUID, List<ArmorStand>> playerHolograms;
    private final Map<UUID, Integer> hologramsTasks;
    private final Map<UUID, Integer> followTasks;
    
    public HologramsManager(FcChat plugin) {
        this.plugin = plugin;
        this.playerHolograms = new HashMap<>();
        this.hologramsTasks = new HashMap<>();
        this.followTasks = new HashMap<>();
    }
    
    public void createHologram(Player player, String message) {
        if (player == null || message == null || !plugin.getConfigManager().isHologramMessagesEnabled()) {
            return;
        }
        
        removeExistingHologram(player);
        
        String formattedMessage = plugin.getConfigManager().getHologramMessageFormat()
                .replace("{message}", message);
        
        List<String> lines = splitMessageIntoLines(formattedMessage, plugin.getConfigManager().getHologramMaxWordsPerLine());
        List<ArmorStand> hologramStands = new ArrayList<>();
        
        Location baseLocation = player.getLocation().add(0, plugin.getConfigManager().getHologramHeight(), 0);
        
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            Location standLocation = baseLocation.clone().add(0, -(i * 0.25), 0);
            
            ArmorStand stand = (ArmorStand) player.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            try {
                stand.setCanPickupItems(false);
            } catch (NoSuchMethodError e) {
            }
            stand.setCustomName(HexUtils.translateAlternateColorCodes(line));
            stand.setCustomNameVisible(true);
            try {
                stand.setMarker(true);
            } catch (NoSuchMethodError e) {
            }
            try {
                stand.setSmall(true);
            } catch (NoSuchMethodError e) {
            }
            try {
                stand.setInvulnerable(true);
            } catch (NoSuchMethodError e) {
            }
            try {
                stand.setCollidable(false);
            } catch (NoSuchMethodError e) {
            }
            
            hologramStands.add(stand);
        }
        
        playerHolograms.put(player.getUniqueId(), hologramStands);

        int followTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, () -> {
            updateHologramPosition(player);
        }, 1L, 1L);
        
        followTasks.put(player.getUniqueId(), followTaskId);
        
        int duration = plugin.getConfigManager().getHologramDuration() * 20;
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            removeHologram(player);
        }, duration);
        
        hologramsTasks.put(player.getUniqueId(), taskId);
    }
    
    private List<String> splitMessageIntoLines(String message, int maxWordsPerLine) {
        List<String> lines = new ArrayList<>();
        String[] words = message.split(" ");
        
        StringBuilder currentLine = new StringBuilder();
        int wordCount = 0;
        
        for (String word : words) {
            if (wordCount >= maxWordsPerLine) {
                if (currentLine.length() > 0) {
                    lines.add(currentLine.toString().trim());
                    currentLine = new StringBuilder();
                    wordCount = 0;
                }
            }
            
            currentLine.append(word).append(" ");
            wordCount++;
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString().trim());
        }
        
        return lines;
    }
    
    private void updateHologramPosition(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        
        List<ArmorStand> stands = playerHolograms.get(player.getUniqueId());
        if (stands == null || stands.isEmpty()) {
            return;
        }
        
        Location playerLocation = player.getLocation();
        Location baseLocation = playerLocation.clone().add(0, plugin.getConfigManager().getHologramHeight(), 0);
        
        for (int i = 0; i < stands.size(); i++) {
            ArmorStand stand = stands.get(i);
            if (stand != null && !stand.isDead()) {
                Location newLocation = baseLocation.clone().add(0, -(i * 0.25), 0);
                stand.teleport(newLocation);
            }
        }
    }
    
    public void removeHologram(Player player) {
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        
        List<ArmorStand> stands = playerHolograms.get(playerId);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (!stand.isDead()) {
                    stand.remove();
                }
            }
            playerHolograms.remove(playerId);
        }
        
        Integer taskId = hologramsTasks.get(playerId);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            hologramsTasks.remove(playerId);
        }
        
        Integer followTaskId = followTasks.get(playerId);
        if (followTaskId != null) {
            Bukkit.getScheduler().cancelTask(followTaskId);
            followTasks.remove(playerId);
        }
    }
    
    public void removeExistingHologram(Player player) {
        if (player != null) {
            removeHologram(player);
        }
    }
    
    public void removeAllHolograms() {
        for (List<ArmorStand> stands : playerHolograms.values()) {
            for (ArmorStand stand : stands) {
                if (!stand.isDead()) {
                    stand.remove();
                }
            }
        }
        playerHolograms.clear();
        
        for (Integer taskId : hologramsTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        hologramsTasks.clear();
        
        for (Integer followTaskId : followTasks.values()) {
            Bukkit.getScheduler().cancelTask(followTaskId);
        }
        followTasks.clear();
    }
    
    public void onPlayerQuit(Player player) {
        if (player != null) {
            removeHologram(player);
        }
    }
}
