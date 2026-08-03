package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.manager.config.ConfigManager;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.json.JSONObject;

public class ToxicityStatistics {
    private final ConfigManager configManager;
    private final AiModerator aiModerator;
    private final File dataFile;
    private YamlConfiguration data;
    private final Map<UUID, List<String>> history = new ConcurrentHashMap<UUID, List<String>>();
    private final Map<UUID, Integer> messageCounter = new ConcurrentHashMap<UUID, Integer>();

    public ToxicityStatistics(ConfigManager configManager, AiModerator aiModerator) {
        this.configManager = configManager;
        this.aiModerator = aiModerator;
        this.dataFile = new File(configManager.getPlugin().getDataFolder(), "core/data.yml");
        this.load();
        configManager.getPlugin().getCompatScheduler().runAsyncTimer(600L, 600L, this::save);
    }

    private void load() {
        if (!this.dataFile.exists()) {
            try {
                File parent = this.dataFile.getParentFile();
                if (parent != null) {
                    parent.mkdirs();
                }
                this.dataFile.createNewFile();
            }
            catch (IOException e) {
                this.configManager.getPlugin().getLogger().severe("Failed to create core/data.yml: " + e.getMessage());
            }
        }
        this.data = YamlConfiguration.loadConfiguration(this.dataFile);
        if (this.data.getConfigurationSection("players") == null) {
            this.data.createSection("players");
        }
    }

    public void record(Player player, int toxicity, String message) {
        if (!this.configManager.isAiModeratorStatisticsEnabled()) {
            return;
        }
        UUID uuid = player.getUniqueId();
        String path = "players." + uuid.toString();
        synchronized (this.data) {
            int total = this.data.getInt(path + ".total-toxicity", 0) + toxicity;
            int count = this.data.getInt(path + ".messages", 0) + 1;
            this.data.set(path + ".name", player.getName());
            this.data.set(path + ".total-toxicity", total);
            this.data.set(path + ".messages", count);
            this.data.set(path + ".average", average(total, count));
        }
        List<String> recent = this.history.computeIfAbsent(uuid, k -> new LinkedList<String>());
        synchronized (recent) {
            recent.add(message);
            int limit = Math.max(1, this.configManager.getAiModeratorStatisticsHistoryLimit());
            while (recent.size() > limit) {
                recent.remove(0);
            }
        }
        int counter = this.messageCounter.getOrDefault(uuid, 0) + 1;
        this.messageCounter.put(uuid, counter);
        int interval = Math.max(1, this.configManager.getAiModeratorStatisticsVerdictInterval());
        if (counter % interval == 0) {
            this.triggerVerdict(player, uuid, recent, average(this.data.getInt(path + ".total-toxicity", 0), this.data.getInt(path + ".messages", 0)));
        }
    }

    private void triggerVerdict(Player player, UUID uuid, List<String> recent, double avg) {
        List<String> snapshot = new ArrayList<String>(recent);
        this.configManager.getPlugin().getCompatScheduler().runAsync(() -> this.computeVerdict(player, uuid, snapshot, avg));
    }

    private void computeVerdict(Player player, UUID uuid, List<String> recent, double avg) {
        try {
            StringBuilder sb = new StringBuilder();
            for (String m : recent) {
                sb.append("- ").append(m).append("\n");
            }
            String historyText = "Average toxicity: " + String.format("%.2f", avg) + "\nRecent messages:\n" + sb.toString();
            String prompt = this.configManager.getVerdictPrompt();
            if (prompt == null || prompt.isEmpty()) {
                return;
            }
            JSONObject result = this.aiModerator.requestJson(prompt, historyText);
            if (result == null) {
                return;
            }
            String verdict = result.optString("verdict", "unknown");
            String summary = result.optString("summary", "");
            String path = "players." + uuid.toString();
            synchronized (this.data) {
                this.data.set(path + ".verdict", verdict);
                this.data.set(path + ".summary", summary);
                this.data.set(path + ".average", avg);
            }
            this.save();
            this.configManager.getPlugin().getLogger().info("[AI-Moderator] Verdict for " + player.getName() + ": " + verdict);
        }
        catch (Exception e) {
            this.configManager.getPlugin().getLogger().warning("Failed to compute AI verdict: " + e.getMessage());
        }
    }

    public double getAverage(UUID uuid) {
        String path = "players." + uuid.toString();
        synchronized (this.data) {
            int total = this.data.getInt(path + ".total-toxicity", 0);
            int count = this.data.getInt(path + ".messages", 0);
            return average(total, count);
        }
    }

    public String getVerdict(UUID uuid) {
        if (!this.configManager.isAiModeratorStatisticsEnabled()) {
            return "N/A";
        }
        synchronized (this.data) {
            return this.data.getString("players." + uuid.toString() + ".verdict", "N/A");
        }
    }

    public void save() {
        if (this.data == null) {
            return;
        }
        synchronized (this.data) {
            try {
                this.data.save(this.dataFile);
            }
            catch (IOException e) {
                this.configManager.getPlugin().getLogger().severe("Failed to save core/data.yml: " + e.getMessage());
            }
        }
    }

    private static double average(int total, int count) {
        return count == 0 ? 0.0 : (double) total / count;
    }
}
