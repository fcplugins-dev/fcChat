package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.data.PlayerTimeManager;
import fc.plugins.fcchat.integration.PlaceholderAPIIntegration;
import fc.plugins.fcchat.utils.HexUtils;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerInfoManager {
    private final ConfigManager configManager;
    private final PlayerTimeManager playerTimeManager;

    public PlayerInfoManager(ConfigManager configManager, PlayerTimeManager playerTimeManager) {
        this.configManager = configManager;
        this.playerTimeManager = playerTimeManager;
    }

    public TextComponent createPlayerInfoComponent(String playerName, String originalText) {
        if (!configManager.isPlayerInfoEnabled()) {
            return new TextComponent(originalText);
        }

        TextComponent component = new TextComponent(originalText);
        
        String hoverText = createPlayerInfoText(playerName);
        
        if (hoverText != null && !hoverText.isEmpty()) {
            HoverEvent hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText).create()
            );
            component.setHoverEvent(hoverEvent);
        }

        return component;
    }

    private String createPlayerInfoText(String playerName) {
        List<String> infoLines = configManager.getPlayerInfoLines();
        if (infoLines.isEmpty()) {
            return null;
        }

        StringBuilder hoverText = new StringBuilder();
        
        for (String line : infoLines) {
            if (hoverText.length() > 0) {
                hoverText.append("\n");
            }
            
            String processedLine = processPlayerInfoLine(line, playerName);
            hoverText.append(processedLine);
        }

        return HexUtils.translateAlternateColorCodes(hoverText.toString());
    }

    private String processPlayerInfoLine(String line, String playerName) {
        String processedLine = line;
        
        PlaceholderAPIIntegration placeholderAPI = configManager.getPlaceholderAPI();
        if (placeholderAPI.isEnabled()) {
            Player targetPlayer = org.bukkit.Bukkit.getPlayer(playerName);
            if (targetPlayer != null) {
                processedLine = placeholderAPI.setPlaceholders(targetPlayer, processedLine);
            }
        }
        
        return processedLine;
    }
} 