package fc.plugins.fcchat.utils.function.chatgame;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.utils.HexUtils;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.scheduler.BukkitTask;

public class ChatGame
        implements Listener {
    private final FcChat plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Random random = new Random();
    private final Map<String, Object> currentGame = new HashMap<String, Object>();
    private BukkitTask gameTask;
    private BukkitTask gameDurationTask;
    private boolean gameActive = false;

    public ChatGame(FcChat plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "chatgame.yml");
        this.loadConfig();
        this.startGameTimer();
    }

    private void loadConfig() {
        if (!this.configFile.exists()) {
            this.plugin.saveResource("chatgame.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
    }

    public void reloadConfig() {
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
        this.stopGameTimer();
        if (this.config.getBoolean("settings.enabled")) {
            this.startGameTimer();
        }
    }

    private void startGameTimer() {
        if (!this.config.getBoolean("settings.enabled")) {
            return;
        }
        int interval = this.config.getInt("settings.timer") * 20;
        this.gameTask = Bukkit.getScheduler().runTaskTimer(this.plugin, this::startRandomGame, interval, interval);
    }

    private void stopGameTimer() {
        if (this.gameTask != null) {
            this.gameTask.cancel();
            this.gameTask = null;
        }
    }

    private void startRandomGame() {
        String gameType;
        if (this.gameActive || !this.config.getBoolean("settings.enabled") || !this.plugin.getConfig().getBoolean("chat.enabled")) {
            return;
        }
        String[] gameTypes = new String[]{"math", "word", "question"};
        switch (gameType = gameTypes[this.random.nextInt(gameTypes.length)]) {
            case "math": {
                this.startMathGame();
                break;
            }
            case "word": {
                this.startWordGame();
                break;
            }
            case "question": {
                this.startQuestionGame();
            }
        }
    }

    private void startMathGame() {
        ConfigurationSection mathSection = this.config.getConfigurationSection("games.math");
        if (mathSection == null) {
            return;
        }
        List<String> problems = mathSection.getStringList("problems");
        if (problems.isEmpty()) {
            return;
        }
        String problem = (String)problems.get(this.random.nextInt(problems.size()));
        String[] parts = problem.split(":");
        if (parts.length != 2) {
            return;
        }
        String expression = parts[0];
        String answer = parts[1];
        this.currentGame.put("type", "math");
        this.currentGame.put("answer", answer);
        this.gameActive = true;
        List<String> startMessages = mathSection.getStringList("start-messages");
        for (String message : startMessages) {
            this.broadcastMessage(message.replace("{expression}", expression));
        }
        this.playSound();
        this.scheduleGameEnd();
    }

    private void startWordGame() {
        ConfigurationSection wordSection = this.config.getConfigurationSection("games.word");
        if (wordSection == null) {
            return;
        }
        List<String> words = wordSection.getStringList("words");
        if (words.isEmpty()) {
            return;
        }
        String word = (String)words.get(this.random.nextInt(words.size()));
        String scrambled = this.scrambleWord(word);
        this.currentGame.put("type", "word");
        this.currentGame.put("answer", word.toLowerCase());
        this.gameActive = true;
        List<String> startMessages = wordSection.getStringList("start-messages");
        for (String message : startMessages) {
            this.broadcastMessage(message.replace("{word}", scrambled));
        }
        this.playSound();
        this.scheduleGameEnd();
    }

    private void startQuestionGame() {
        ConfigurationSection questionSection = this.config.getConfigurationSection("games.question");
        if (questionSection == null) {
            return;
        }
        List<String> questions = questionSection.getStringList("questions");
        if (questions.isEmpty()) {
            return;
        }
        String questionAnswer = (String)questions.get(this.random.nextInt(questions.size()));
        String[] parts = questionAnswer.split(":");
        if (parts.length != 2) {
            return;
        }
        String question = parts[0];
        String answer = parts[1];
        this.currentGame.put("type", "question");
        this.currentGame.put("answer", answer.toLowerCase());
        this.gameActive = true;
        List<String> startMessages = questionSection.getStringList("start-messages");
        for (String message : startMessages) {
            this.broadcastMessage(message.replace("{question}", question));
        }
        this.playSound();
        this.scheduleGameEnd();
    }

    private String scrambleWord(String word) {
        char[] chars = word.toCharArray();
        for (int i = 0; i < chars.length; ++i) {
            int randomIndex = this.random.nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[randomIndex];
            chars[randomIndex] = temp;
        }
        return new String(chars);
    }

    private void scheduleGameEnd() {
        int duration = this.config.getInt("settings.game-duration") * 20;
        this.gameDurationTask = Bukkit.getScheduler().runTaskLater(this.plugin, this::endGame, duration);
    }

    private void endGame() {
        if (!this.gameActive) {
            return;
        }
        String gameType = (String)this.currentGame.get("type");
        ConfigurationSection section = this.config.getConfigurationSection("games." + gameType);
        if (section != null) {
            List<String> endMessages = section.getStringList("end-messages");
            String answer = (String)this.currentGame.get("answer");
            for (String message : endMessages) {
                this.broadcastMessage(message.replace("{answer}", answer));
            }
        }
        this.gameActive = false;
        this.currentGame.clear();
        if (this.gameDurationTask != null) {
            this.gameDurationTask.cancel();
            this.gameDurationTask = null;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!this.gameActive || !this.plugin.getConfig().getBoolean("chat.enabled")) {
            return;
        }
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase().trim();
        String answer = (String)this.currentGame.get("answer");
        if (answer != null && message.equals(answer)) {
            event.setCancelled(true);
            String gameType = (String)this.currentGame.get("type");
            ConfigurationSection section = this.config.getConfigurationSection("games." + gameType);
            if (section != null) {
                List<String> winMessages = section.getStringList("win-messages");
                for (String winMessage : winMessages) {
                    this.broadcastMessage(winMessage.replace("{player}", player.getName()).replace("{answer}", answer));
                }
                this.executeRewards(player, section);
            }
            this.gameActive = false;
            this.currentGame.clear();
            if (this.gameDurationTask != null) {
                this.gameDurationTask.cancel();
                this.gameDurationTask = null;
            }
        }
    }

    private void executeRewards(Player player, ConfigurationSection section) {
        List<String> rewards = section.getStringList("rewards");
        for (String reward : rewards) {
            Bukkit.getScheduler().runTask(this.plugin, () -> Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("{player}", player.getName())));
        }
    }

    private void broadcastMessage(String message) {
        String formattedMessage = HexUtils.translateAlternateColorCodes(message);
        Bukkit.broadcastMessage(formattedMessage);
    }

    private void playSound() {
        String soundName = this.config.getString("settings.sound.type");
        float volume = (float)this.config.getDouble("settings.sound.volume");
        float pitch = (float)this.config.getDouble("settings.sound.pitch");
        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {}
    }

    public void stop() {
        this.stopGameTimer();
        this.gameActive = false;
        this.currentGame.clear();
        if (this.gameDurationTask != null) {
            this.gameDurationTask.cancel();
            this.gameDurationTask = null;
        }
    }
}
