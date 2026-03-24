package fc.plugins.fcchat.manager.holograms;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.utils.HexUtils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

public class HologramsManager {
    private final FcChat plugin;
    private final Map<UUID, List<ArmorStand>> playerHolograms;
    private final Map<UUID, Integer> hologramsTasks;
    private final Map<UUID, Integer> followTasks;

    public HologramsManager(FcChat plugin) {
        this.plugin = plugin;
        this.playerHolograms = new HashMap<UUID, List<ArmorStand>>();
        this.hologramsTasks = new HashMap<UUID, Integer>();
        this.followTasks = new HashMap<UUID, Integer>();
    }

    public void createHologram(Player player, String message) {
        if (player == null || message == null || !this.plugin.getConfigManager().isHologramMessagesEnabled()) {
            return;
        }
        this.removeExistingHologram(player);
        String formattedMessage = this.plugin.getConfigManager().getHologramMessageFormat().replace("{message}", message);
        List<String> lines = this.splitMessageIntoLines(formattedMessage, this.plugin.getConfigManager().getHologramMaxWordsPerLine());
        ArrayList<ArmorStand> hologramStands = new ArrayList<ArmorStand>();
        Location baseLocation = player.getLocation().add(0.0, this.plugin.getConfigManager().getHologramHeight(), 0.0);
        for (int i = 0; i < lines.size(); ++i) {
            String line = lines.get(i);
            Location standLocation = baseLocation.clone().add(0.0, -((double)i * 0.25), 0.0);
            ArmorStand stand = (ArmorStand)((Object)player.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND));
            stand.setVisible(false);
            stand.setGravity(false);
            try {
                stand.setCanPickupItems(false);
            } catch (NoSuchMethodError ignored) {}
            stand.setCustomName(HexUtils.translateAlternateColorCodes(line));
            stand.setCustomNameVisible(true);
            try {
                stand.setMarker(true);
            } catch (NoSuchMethodError ignored) {}
            try {
                stand.setSmall(true);
            } catch (NoSuchMethodError ignored) {}
            try {
                stand.setInvulnerable(true);
            } catch (NoSuchMethodError ignored) {}
            try {
                stand.setCollidable(false);
            } catch (NoSuchMethodError ignored) {}
            hologramStands.add(stand);
        }
        this.playerHolograms.put(player.getUniqueId(), hologramStands);
        int followTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, () -> this.updateHologramPosition(player), 2L, 2L);
        this.followTasks.put(player.getUniqueId(), followTaskId);
        int duration = this.plugin.getConfigManager().getHologramDuration() * 20;
        int taskId = Bukkit.getScheduler().scheduleSyncDelayedTask(this.plugin, () -> this.removeHologram(player), duration);
        this.hologramsTasks.put(player.getUniqueId(), taskId);
    }

    private List<String> splitMessageIntoLines(String message, int maxWordsPerLine) {
        ArrayList<String> lines = new ArrayList<String>();
        String[] words = message.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int wordCount = 0;
        for (String word : words) {
            if (wordCount >= maxWordsPerLine && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder();
                wordCount = 0;
            }
            currentLine.append(word).append(" ");
            ++wordCount;
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
        List<ArmorStand> stands = this.playerHolograms.get(player.getUniqueId());
        if (stands == null || stands.isEmpty()) {
            return;
        }
        Location playerLocation = player.getLocation();
        Location baseLocation = playerLocation.clone().add(0.0, this.plugin.getConfigManager().getHologramHeight(), 0.0);
        for (int i = 0; i < stands.size(); ++i) {
            ArmorStand stand = stands.get(i);
            if (stand == null || stand.isDead()) continue;
            Location newLocation = baseLocation.clone().add(0.0, -((double)i * 0.25), 0.0);
            stand.teleport(newLocation);
        }
    }

    public void removeHologram(Player player) {
        Integer followTaskId;
        Integer taskId;
        if (player == null) {
            return;
        }
        UUID playerId = player.getUniqueId();
        List<ArmorStand> stands = this.playerHolograms.get(playerId);
        if (stands != null) {
            for (ArmorStand stand : stands) {
                if (stand.isDead()) continue;
                stand.remove();
            }
            this.playerHolograms.remove(playerId);
        }
        if ((taskId = this.hologramsTasks.get(playerId)) != null) {
            Bukkit.getScheduler().cancelTask(taskId);
            this.hologramsTasks.remove(playerId);
        }
        if ((followTaskId = this.followTasks.get(playerId)) != null) {
            Bukkit.getScheduler().cancelTask(followTaskId);
            this.followTasks.remove(playerId);
        }
    }

    public void removeExistingHologram(Player player) {
        if (player != null) {
            this.removeHologram(player);
        }
    }

    public void removeAllHolograms() {
        for (List<ArmorStand> stands : this.playerHolograms.values()) {
            for (ArmorStand stand : stands) {
                if (stand.isDead()) continue;
                stand.remove();
            }
        }
        this.playerHolograms.clear();
        for (Integer taskId : this.hologramsTasks.values()) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        this.hologramsTasks.clear();
        for (Integer followTaskId : this.followTasks.values()) {
            Bukkit.getScheduler().cancelTask(followTaskId);
        }
        this.followTasks.clear();
    }

    public void onPlayerQuit(Player player) {
        if (player != null) {
            this.removeHologram(player);
        }
    }
}
