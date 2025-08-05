package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.config.ConfigManager;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import org.bukkit.ChatColor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MessageProcessor {
    private static final Pattern HIDDEN_TEXT_PATTERN = Pattern.compile("\\|\\|([^|]+)\\|\\|");
    
    public static String processHiddenText(String message, ConfigManager configManager) {
        if (!configManager.isHiddenTextEnabled()) {
            return message;
        }
        
        Matcher matcher = HIDDEN_TEXT_PATTERN.matcher(message);
        StringBuffer result = new StringBuffer();
        
        while (matcher.find()) {
            String hiddenText = matcher.group(1);
            String symbol = configManager.getHiddenTextSymbol();
            int length = configManager.getHiddenTextLength();
            String replacement = symbol.repeat(length);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);
        
        return result.toString();
    }
    
    public static TextComponent createHiddenTextComponent(String formattedMessage, String originalMessage, ConfigManager configManager) {
        if (!configManager.isHiddenTextEnabled()) {
            return new TextComponent(formattedMessage);
        }
        
        Matcher matcher = HIDDEN_TEXT_PATTERN.matcher(originalMessage);
        List<String> hiddenTexts = new ArrayList<>();
        
        while (matcher.find()) {
            String hiddenText = matcher.group(1);
            hiddenTexts.add(hiddenText);
        }
        
        TextComponent component = new TextComponent(formattedMessage);
        
        if (!hiddenTexts.isEmpty()) {
            StringBuilder hoverText = new StringBuilder();
            for (String hiddenText : hiddenTexts) {
                if (hoverText.length() > 0) {
                    hoverText.append("\n");
                }
                hoverText.append(hiddenText);
            }
            
            HoverEvent hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText.toString()).create()
            );
            component.setHoverEvent(hoverEvent);
        }
        
        return component;
    }
    
    public static HoverEvent createHiddenTextHover(String originalMessage, ConfigManager configManager) {
        if (!configManager.isHiddenTextEnabled()) {
            return null;
        }
        
        Matcher matcher = HIDDEN_TEXT_PATTERN.matcher(originalMessage);
        List<String> hiddenTexts = new ArrayList<>();
        
        while (matcher.find()) {
            String hiddenText = matcher.group(1);
            hiddenTexts.add(hiddenText);
        }
        
        if (!hiddenTexts.isEmpty()) {
            StringBuilder hoverText = new StringBuilder();
            for (String hiddenText : hiddenTexts) {
                if (hoverText.length() > 0) {
                    hoverText.append("\n");
                }
                hoverText.append(hiddenText);
            }
            
            return new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hoverText.toString()).create()
            );
        }
        
        return null;
    }
} 