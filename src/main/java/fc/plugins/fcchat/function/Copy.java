package fc.plugins.fcchat.function;

import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;

public class Copy {
    private final ConfigManager configManager;

    public Copy(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public TextComponent createClickableMessage(String message, String originalText) {
        TextComponent component = new TextComponent(TextComponent.fromLegacyText(message));
        component.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, originalText));
        String hoverText = configManager.getCopyHoverText();
        if (hoverText != null && !hoverText.isEmpty()) {
            String coloredHoverText = HexUtils.translateAlternateColorCodes(hoverText);
            HoverEvent hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(coloredHoverText).create()
            );
            component.setHoverEvent(hoverEvent);
        }
        
        return component;
    }
    
    public TextComponent addClickEvent(TextComponent component, String originalText) {
        component.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, originalText));
        String hoverText = configManager.getCopyHoverText();
        if (hoverText != null && !hoverText.isEmpty()) {
            String coloredHoverText = HexUtils.translateAlternateColorCodes(hoverText);
            HoverEvent hoverEvent = new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(coloredHoverText).create()
            );
            component.setHoverEvent(hoverEvent);
        }
        
        return component;
    }
} 