
package fc.plugins.fcchat.manager.holograms;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.utils.concurrent.CompatScheduler;
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
    private final Map<UUID, CompatScheduler.ScheduledTask> hologramsTasks;
    private final Map<UUID, CompatScheduler.ScheduledTask> followTasks;

    public HologramsManager(FcChat plugin) {
        this.plugin = plugin;
        this.playerHolograms = new HashMap<UUID, List<ArmorStand>>();
        this.hologramsTasks = new HashMap<UUID, CompatScheduler.ScheduledTask>();
        this.followTasks = new HashMap<UUID, CompatScheduler.ScheduledTask>();
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
        int i = 0;
        while (i < lines.size()) {
            String line = lines.get(i);
            Location standLocation = baseLocation.clone().add(0.0, -((double)i * 0.25), 0.0);
            ArmorStand stand = (ArmorStand)player.getWorld().spawnEntity(standLocation, EntityType.ARMOR_STAND);
            stand.setVisible(false);
            stand.setGravity(false);
            try {
                stand.setCanPickupItems(false);
            }
            catch (NoSuchMethodError noSuchMethodError) {
                // empty catch block
            }
            stand.setCustomName(HexUtils.translateAlternateColorCodes(line));
            stand.setCustomNameVisible(true);
            try {
                stand.setMarker(true);
            }
            catch (NoSuchMethodError noSuchMethodError) {
                // empty catch block
            }
            try {
                stand.setSmall(true);
            }
            catch (NoSuchMethodError noSuchMethodError) {
                // empty catch block
            }
            try {
                stand.setInvulnerable(true);
            }
            catch (NoSuchMethodError noSuchMethodError) {
                // empty catch block
            }
            try {
                stand.setCollidable(false);
            }
            catch (NoSuchMethodError noSuchMethodError) {
                // empty catch block
            }
            hologramStands.add(stand);
            ++i;
        }
        this.playerHolograms.put(player.getUniqueId(), hologramStands);
        CompatScheduler.ScheduledTask followTaskId = this.plugin.getCompatScheduler().runEntityTimer(player, 2L, 2L, () -> this.updateHologramPosition(player));
        this.followTasks.put(player.getUniqueId(), followTaskId);
        int duration = this.plugin.getConfigManager().getHologramDuration() * 20;
        CompatScheduler.ScheduledTask taskId = this.plugin.getCompatScheduler().runEntityLater(player, duration, () -> this.removeHologram(player));
        this.hologramsTasks.put(player.getUniqueId(), taskId);
    }

    private List<String> splitMessageIntoLines(String message, int maxWordsPerLine) {
        ArrayList<String> lines = new ArrayList<String>();
        String[] words = message.split(" ");
        StringBuilder currentLine = new StringBuilder();
        int wordCount = 0;
        String[] stringArray = words;
        int n = words.length;
        int n2 = 0;
        while (n2 < n) {
            String word = stringArray[n2];
            if (wordCount >= maxWordsPerLine && currentLine.length() > 0) {
                lines.add(currentLine.toString().trim());
                currentLine = new StringBuilder();
                wordCount = 0;
            }
            currentLine.append(word).append(" ");
            ++wordCount;
            ++n2;
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
        int i = 0;
        while (i < stands.size()) {
            ArmorStand stand = stands.get(i);
            if (stand != null && !stand.isDead()) {
                Location newLocation = baseLocation.clone().add(0.0, -((double)i * 0.25), 0.0);
                stand.teleport(newLocation);
            }
            ++i;
        }
    }

    public void removeHologram(Player player) {
        CompatScheduler.ScheduledTask followTaskId;
        CompatScheduler.ScheduledTask taskId;
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
            taskId.cancel();
            this.hologramsTasks.remove(playerId);
        }
        if ((followTaskId = this.followTasks.get(playerId)) != null) {
            followTaskId.cancel();
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
        for (CompatScheduler.ScheduledTask taskId : this.hologramsTasks.values()) {
            taskId.cancel();
        }
        this.hologramsTasks.clear();
        for (CompatScheduler.ScheduledTask followTaskId : this.followTasks.values()) {
            followTaskId.cancel();
        }
        this.followTasks.clear();
    }

    public void onPlayerQuit(Player player) {
        if (player != null) {
            this.removeHologram(player);
        }
    }
}
