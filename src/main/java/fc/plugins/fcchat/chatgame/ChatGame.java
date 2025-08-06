package fc.plugins.fcchat.chatgame;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.utils.HexUtils;
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

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class ChatGame implements Listener {
    private final FcChat plugin;
    private final File configFile;
    private FileConfiguration config;
    private final Random random = new Random();
    private final Map<String, Object> currentGame = new HashMap<>();
    private BukkitTask gameTask;
    private BukkitTask gameDurationTask;
    private boolean gameActive = false;

    public ChatGame(FcChat plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "chatgame.yml");
        loadConfig();
        startGameTimer();
    }

    private void loadConfig() {
        if (!configFile.exists()) {
            plugin.saveResource("chatgame.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void reloadConfig() {
        config = YamlConfiguration.loadConfiguration(configFile);
        stopGameTimer();
        startGameTimer();
    }

    private void startGameTimer() {
        int interval = config.getInt("settings.timer") * 20;
        gameTask = Bukkit.getScheduler().runTaskTimer(plugin, this::startRandomGame, interval, interval);
    }

    private void stopGameTimer() {
        if (gameTask != null) {
            gameTask.cancel();
            gameTask = null;
        }
    }

    private void startRandomGame() {
        if (gameActive || !config.getBoolean("settings.enabled")) return;
        
        String[] gameTypes = {"math", "word", "question"};
        String gameType = gameTypes[random.nextInt(gameTypes.length)];
        
        switch (gameType) {
            case "math":
                startMathGame();
                break;
            case "word":
                startWordGame();
                break;
            case "question":
                startQuestionGame();
                break;
        }
    }

    private void startMathGame() {
        ConfigurationSection mathSection = config.getConfigurationSection("games.math");
        if (mathSection == null) return;
        
        List<String> problems = mathSection.getStringList("problems");
        if (problems.isEmpty()) return;
        
        String problem = problems.get(random.nextInt(problems.size()));
        String[] parts = problem.split(":");
        if (parts.length != 2) return;
        
        String expression = parts[0];
        String answer = parts[1];
        
        currentGame.put("type", "math");
        currentGame.put("answer", answer);
        gameActive = true;
        
        List<String> startMessages = mathSection.getStringList("start-messages");
        
        for (String message : startMessages) {
            broadcastMessage(message.replace("{expression}", expression));
        }
        
        playSound();
        scheduleGameEnd();
    }

    private void startWordGame() {
        ConfigurationSection wordSection = config.getConfigurationSection("games.word");
        if (wordSection == null) return;
        
        List<String> words = wordSection.getStringList("words");
        if (words.isEmpty()) return;
        
        String word = words.get(random.nextInt(words.size()));
        String scrambled = scrambleWord(word);
        
        currentGame.put("type", "word");
        currentGame.put("answer", word.toLowerCase());
        gameActive = true;
        
        List<String> startMessages = wordSection.getStringList("start-messages");
        
        for (String message : startMessages) {
            broadcastMessage(message.replace("{word}", scrambled));
        }
        
        playSound();
        scheduleGameEnd();
    }

    private void startQuestionGame() {
        ConfigurationSection questionSection = config.getConfigurationSection("games.question");
        if (questionSection == null) return;
        
        List<String> questions = questionSection.getStringList("questions");
        if (questions.isEmpty()) return;
        
        String questionAnswer = questions.get(random.nextInt(questions.size()));
        String[] parts = questionAnswer.split(":");
        if (parts.length != 2) return;
        
        String question = parts[0];
        String answer = parts[1];
        
        currentGame.put("type", "question");
        currentGame.put("answer", answer.toLowerCase());
        gameActive = true;
        
        List<String> startMessages = questionSection.getStringList("start-messages");
        
        for (String message : startMessages) {
            broadcastMessage(message.replace("{question}", question));
        }
        
        playSound();
        scheduleGameEnd();
    }

    private String scrambleWord(String word) {
        char[] chars = word.toCharArray();
        for (int i = 0; i < chars.length; i++) {
            int randomIndex = random.nextInt(chars.length);
            char temp = chars[i];
            chars[i] = chars[randomIndex];
            chars[randomIndex] = temp;
        }
        return new String(chars);
    }

    private void scheduleGameEnd() {
        int duration = config.getInt("settings.game-duration") * 20;
        gameDurationTask = Bukkit.getScheduler().runTaskLater(plugin, this::endGame, duration);
    }

    private void endGame() {
        if (!gameActive) return;
        
        String gameType = (String) currentGame.get("type");
        ConfigurationSection section = config.getConfigurationSection("games." + gameType);
        if (section != null) {
            List<String> endMessages = section.getStringList("end-messages");
            String answer = (String) currentGame.get("answer");
            
            for (String message : endMessages) {
                broadcastMessage(message.replace("{answer}", answer));
            }
        }
        
        gameActive = false;
        currentGame.clear();
        
        if (gameDurationTask != null) {
            gameDurationTask.cancel();
            gameDurationTask = null;
        }
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (!gameActive) return;
        
        Player player = event.getPlayer();
        String message = event.getMessage().toLowerCase().trim();
        String answer = (String) currentGame.get("answer");
        
        if (answer != null && message.equals(answer)) {
            event.setCancelled(true);
            
            String gameType = (String) currentGame.get("type");
            ConfigurationSection section = config.getConfigurationSection("games." + gameType);
            if (section != null) {
                List<String> winMessages = section.getStringList("win-messages");
                for (String winMessage : winMessages) {
                    broadcastMessage(winMessage.replace("{player}", player.getName()).replace("{answer}", answer));
                }
                
                executeRewards(player, section);
            }
            
            gameActive = false;
            currentGame.clear();
            
            if (gameDurationTask != null) {
                gameDurationTask.cancel();
                gameDurationTask = null;
            }
        }
    }

    private void executeRewards(Player player, ConfigurationSection section) {
        List<String> rewards = section.getStringList("rewards");
        for (String reward : rewards) {
            Bukkit.getScheduler().runTask(plugin, () -> {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), reward.replace("{player}", player.getName()));
            });
        }
    }

    private void broadcastMessage(String message) {
        String formattedMessage = HexUtils.translateAlternateColorCodes(message);
        Bukkit.broadcastMessage(formattedMessage);
    }

    private void playSound() {
        String soundName = config.getString("settings.sound.type");
        float volume = (float) config.getDouble("settings.sound.volume");
        float pitch = (float) config.getDouble("settings.sound.pitch");
        
        try {
            Sound sound = Sound.valueOf(soundName);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, volume, pitch);
            }
        } catch (IllegalArgumentException e) {
        }
    }

    public void stop() {
        stopGameTimer();
        gameActive = false;
        currentGame.clear();
        
        if (gameDurationTask != null) {
            gameDurationTask.cancel();
            gameDurationTask = null;
        }
    }
}