package fc.plugins.fcchat.utils;

import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class HexUtils {
    private static final MiniMessage MINI_MESSAGE = createMiniMessage();
    private static final LegacyComponentSerializer LEGACY_AMP = createLegacySerializer('&');
    private static final LegacyComponentSerializer LEGACY_SECTION = createLegacySerializer('§');
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("&#([A-Fa-f0-9]{6})");
    private static final Pattern ANGLE_GRADIENT_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>(.*?)</#([A-Fa-f0-9]{6})>", Pattern.DOTALL);
    private static final Pattern RAW_HEX_PATTERN = Pattern.compile("(?<![<&])#([A-Fa-f0-9]{6})");

    public static String translateAlternateColorCodes(String message) {
        if (message == null) {
            return null;
        }

        if (MINI_MESSAGE != null) {
            try {
                String prepared = prepareMiniMessageSyntax(message);
                Component legacyParsed = LEGACY_AMP.deserialize(prepared);
                String miniCandidate = MINI_MESSAGE.serialize(legacyParsed)
                    .replace("\\<", "<")
                    .replace("\\>", ">");
                Component finalComponent = MINI_MESSAGE.deserialize(miniCandidate);
                return LEGACY_SECTION.serialize(finalComponent);
            } catch (Exception ignored) {
            }
        }

        try {
            return LEGACY_SECTION.serialize(LEGACY_AMP.deserialize(message));
        } catch (Exception ignoredAgain) {
            return message;
        }
    }

    private static MiniMessage createMiniMessage() {
        try {
            Method miniMessageFactory = MiniMessage.class.getMethod("miniMessage");
            return (MiniMessage) miniMessageFactory.invoke(null);
        } catch (Throwable ignored) {
            try {
                Method legacyFactory = MiniMessage.class.getMethod("get");
                return (MiniMessage) legacyFactory.invoke(null);
            } catch (Throwable ignoredAgain) {
                return null;
            }
        }
    }

    private static LegacyComponentSerializer createLegacySerializer(char character) {
        try {
            return LegacyComponentSerializer.builder()
                .character(character)
                .hexColors()
                .useUnusualXRepeatedCharacterHexFormat()
                .build();
        } catch (Throwable ignored) {
            return LegacyComponentSerializer.builder()
                .character(character)
                .build();
        }
    }

    private static String prepareMiniMessageSyntax(String message) {
        String prepared = message
            .replace("<br>", "\n")
            .replace("<br/>", "\n")
            .replace("<p>", "\n")
            .replace("</p>", "\n");
        prepared = LEGACY_HEX_PATTERN.matcher(prepared).replaceAll("<#$1>");
        prepared = RAW_HEX_PATTERN.matcher(prepared).replaceAll("<#$1>");

        Matcher gradientMatcher = ANGLE_GRADIENT_PATTERN.matcher(prepared);
        StringBuilder sb = new StringBuilder();
        int lastEnd = 0;
        while (gradientMatcher.find()) {
            sb.append(prepared, lastEnd, gradientMatcher.start());
            String start = gradientMatcher.group(1);
            String text = gradientMatcher.group(2);
            String end = gradientMatcher.group(3);
            String replacement = "<gradient:#" + start + ":#" + end + ">" + text + "</gradient>";
            sb.append(replacement);
            lastEnd = gradientMatcher.end();
        }
        sb.append(prepared, lastEnd, prepared.length());
        return sb.toString();
    }
}
