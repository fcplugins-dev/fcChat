
package fc.plugins.fcchat.utils.function;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.HexUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Copy {
    private final ConfigManager configManager;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder().character('§').hexColors().useUnusualXRepeatedCharacterHexFormat().build();

    public Copy(ConfigManager configManager) {
        this.configManager = configManager;
    }

    public Component createClickableMessage(String message, String originalText) {
        Component component = LEGACY.deserialize(message).clickEvent(ClickEvent.copyToClipboard(originalText));
        String hoverText = this.configManager.getCopyHoverText();
        if (hoverText != null && !hoverText.isEmpty() && component.hoverEvent() == null) {
            String coloredHoverText = HexUtils.translateAlternateColorCodes(hoverText);
            component = component.hoverEvent(HoverEvent.showText(LEGACY.deserialize(coloredHoverText)));
        }
        return component;
    }

    public Component addClickEvent(Component component, String originalText) {
        component = component.clickEvent(ClickEvent.copyToClipboard(originalText));
        String hoverText = this.configManager.getCopyHoverText();
        if (hoverText != null && !hoverText.isEmpty() && component.hoverEvent() == null) {
            String coloredHoverText = HexUtils.translateAlternateColorCodes(hoverText);
            component = component.hoverEvent(HoverEvent.showText(LEGACY.deserialize(coloredHoverText)));
        }
        return component;
    }
}
