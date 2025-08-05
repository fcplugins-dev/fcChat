package fc.plugins.fcchat.moderation;

import fc.plugins.fcchat.config.ConfigManager;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;

public class Filter {
    private final ConfigManager configManager;
    private FileConfiguration filterConfig;
    private File filterFile;

    public Filter(ConfigManager configManager) {
        this.configManager = configManager;
        loadFilterConfig();
    }

    private void loadFilterConfig() {
        filterFile = new File(configManager.getPlugin().getDataFolder(), "moderation.yml");
        if (!filterFile.exists()) {
            configManager.getPlugin().saveResource("moderation.yml", false);
        }
        filterConfig = YamlConfiguration.loadConfiguration(filterFile);
    }

    public void reloadFilter() {
        loadFilterConfig();
    }

    public boolean isFilterEnabled() {
        return filterConfig.getBoolean("filter.enabled", true);
    }

    public String getFilterMode() {
        return filterConfig.getString("filter.mode", "partial");
    }

    public String getFilterSymbol() {
        return filterConfig.getString("filter.symbol", "*");
    }

    public String getCustomReplacement() {
        return filterConfig.getString("filter.custom-replacement", "***");
    }

    public String filterMessage(String message, Player player) {
        if (!isFilterEnabled()) {
            return message;
        }

        if (player.hasPermission("fcchat.bypass")) {
            return message;
        }

        List<String> badWords = filterConfig.getStringList("filter.bad-words");
        String filteredMessage = message;

        for (String badWord : badWords) {
            if (filteredMessage.toLowerCase().contains(badWord.toLowerCase())) {
                filteredMessage = replaceWord(filteredMessage, badWord);
            }
        }

        return filteredMessage;
    }

    public boolean wasMessageFiltered(String originalMessage, String filteredMessage) {
        return !originalMessage.equals(filteredMessage);
    }

    private String replaceWord(String message, String badWord) {
        String lowerMessage = message.toLowerCase();
        String lowerBadWord = badWord.toLowerCase();
        
        int startIndex = lowerMessage.indexOf(lowerBadWord);
        if (startIndex == -1) {
            return message;
        }

        String before = message.substring(0, startIndex);
        String after = message.substring(startIndex + badWord.length());
        String originalWord = message.substring(startIndex, startIndex + badWord.length());

        String censoredWord = censorWord(originalWord);

        return before + censoredWord + after;
    }

    private String censorWord(String word) {
        String mode = getFilterMode();
        String symbol = getFilterSymbol();

        switch (mode) {
            case "partial":
                return censorPartial(word, symbol);
            case "full":
                return censorFull(word, symbol);
            case "custom":
                return getCustomReplacement();
            default:
                return censorPartial(word, symbol);
        }
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
        if (!isFilterEnabled()) {
            return false;
        }

        List<String> badWords = filterConfig.getStringList("filter.bad-words");
        String lowerMessage = message.toLowerCase();

        for (String badWord : badWords) {
            if (lowerMessage.contains(badWord.toLowerCase())) {
                return true;
            }
        }

        return false;
    }
} 