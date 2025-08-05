package fc.plugins.fcchat.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexUtils {
    private static Boolean supported = null;

    public static String translateAlternateColorCodes(String message) {
        if (message == null) {
            return null;
        }

        if (isNotSupported()) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        message = translateHexColorCodes(message);
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    public static String translateHexColorCodes(String message) {
        if (message == null) {
            return null;
        }

        if (isNotSupported()) {
            return message;
        }

        message = translateSingleMessageColorCodes(message);
        message = translateGradientCodes(message);

        return message;
    }

    public static String translateSingleMessageColorCodes(String message) {
        for (int i = 0; i < message.length(); i++) {
            if (message.length() - i > 8) {
                String tempString = message.substring(i, i + 8);
                if (tempString.startsWith("&#")) {
                    char[] tempChars = tempString.replaceFirst("&#", "").toCharArray();
                    StringBuilder rgbColor = new StringBuilder();
                    rgbColor.append("§x");
                    
                    for (char tempChar : tempChars) {
                        rgbColor.append("§").append(tempChar);
                    }

                    message = message.replaceAll(tempString, rgbColor.toString());
                }
            }
        }

        return message;
    }

    public static String translateGradientCodes(String message) {
        Pattern hexPattern = Pattern.compile("#([A-Fa-f0-9]{6})");
        Matcher matcher = hexPattern.matcher(message);
        StringBuffer buffer = new StringBuffer(message.length() + 32);

        while (matcher.find()) {
            String group = matcher.group(1);
            matcher.appendReplacement(buffer, "§x§" + group.charAt(0) + "§" + group.charAt(1) + "§" + group.charAt(2) + "§" + group.charAt(3) + "§" + group.charAt(4) + "§" + group.charAt(5));
        }

        return matcher.appendTail(buffer).toString();
    }

    private static boolean isNotSupported() {
        if (supported == null) {
            try {
                String version = Bukkit.getVersion();
                String ver = version.split("\\(MC: ")[1];
                String[] numbers = ver.replaceAll("\\)", "").split("\\.");
                ver = numbers[0] + numbers[1];
                int toCheck = Integer.valueOf(ver);
                supported = toCheck >= 116;
            } catch (Exception e) {
                supported = false;
            }
        }

        return !supported;
    }
} 