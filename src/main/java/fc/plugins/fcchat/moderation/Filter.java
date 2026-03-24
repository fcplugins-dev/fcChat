package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.manager.config.ConfigManager;
import java.io.File;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class Filter {
    private final ConfigManager configManager;
    private FileConfiguration filterConfig;
    private File filterFile;

    public Filter(ConfigManager configManager) {
        this.configManager = configManager;
        this.loadFilterConfig();
    }

    private void loadFilterConfig() {
        this.filterFile = new File(this.configManager.getPlugin().getDataFolder(), "moderation.yml");
        if (!this.filterFile.exists()) {
            this.configManager.getPlugin().saveResource("moderation.yml", false);
        }
        this.filterConfig = YamlConfiguration.loadConfiguration(this.filterFile);
    }

    public void reloadFilter() {
        this.loadFilterConfig();
    }

    public boolean isFilterEnabled() {
        return this.filterConfig.getBoolean("filter.enabled", true);
    }

    public String getFilterMode() {
        return this.filterConfig.getString("filter.mode", "partial");
    }

    public String getFilterSymbol() {
        return this.filterConfig.getString("filter.symbol", "*");
    }

    public String getCustomReplacement() {
        return this.filterConfig.getString("filter.custom-replacement", "***");
    }

    public String filterMessage(String message, Player player) {
        if (!this.isFilterEnabled()) {
            return message;
        }
        if (player.hasPermission("fcchat.bypass")) {
            return message;
        }
        List<String> badWords = this.filterConfig.getStringList("filter.bad-words");
        String filteredMessage = message;
        for (String badWord : badWords) {
            if (!filteredMessage.toLowerCase().contains(badWord.toLowerCase())) continue;
            filteredMessage = this.replaceWord(filteredMessage, badWord);
        }
        return filteredMessage;
    }

    public boolean wasMessageFiltered(String originalMessage, String filteredMessage) {
        return !originalMessage.equals(filteredMessage);
    }

    private String replaceWord(String message, String badWord) {
        String lowerBadWord;
        String lowerMessage = message.toLowerCase();
        int startIndex = lowerMessage.indexOf(lowerBadWord = badWord.toLowerCase());
        if (startIndex == -1) {
            return message;
        }
        String before = message.substring(0, startIndex);
        String after = message.substring(startIndex + badWord.length());
        String originalWord = message.substring(startIndex, startIndex + badWord.length());
        String censoredWord = this.censorWord(originalWord);
        return before + censoredWord + after;
    }

    private String censorWord(String word) {
        String mode = this.getFilterMode();
        String symbol = this.getFilterSymbol();
        switch (mode) {
            case "partial": {
                return this.censorPartial(word, symbol);
            }
            case "full": {
                return this.censorFull(word, symbol);
            }
            case "custom": {
                return this.getCustomReplacement();
            }
        }
        return this.censorPartial(word, symbol);
    }

    private String censorPartial(String word, String symbol) {
        if (word.length() <= 2) {
            return symbol.repeat(word.length());
        }
        return word.charAt(0) + symbol.repeat(word.length() - 2) + word.charAt(word.length() - 1);
    }

    private String censorFull(String word, String symbol) {
        return symbol.repeat(word.length());
    }

    public boolean isBlocked(String message) {
        if (!this.isFilterEnabled()) {
            return false;
        }
        List<String> badWords = this.filterConfig.getStringList("filter.bad-words");
        String lowerMessage = message.toLowerCase();
        for (String badWord : badWords) {
            if (!lowerMessage.contains(badWord.toLowerCase())) continue;
            return true;
        }
        return false;
    }
}
