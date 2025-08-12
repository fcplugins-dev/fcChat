package fc.plugins.fcchat.utils;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HexUtils {
    private static Boolean supported = null;
    private static final Pattern HEX_PATTERN = Pattern.compile("#([A-Fa-f0-9]{6})");
    private static final Pattern GRADIENT_PATTERN = Pattern.compile("<#([A-Fa-f0-9]{6})>([^<]+)</#([A-Fa-f0-9]{6})>");

    public static String translateAlternateColorCodes(String message) {
        if (message == null) {
            return null;
        }

        if (isNotSupported()) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        try {
            message = translateHexPattern(message);
            message = translateGradientCodes(message);
            message = translateSingleMessageColorCodes(message);
            message = ChatColor.translateAlternateColorCodes('&', message);
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', message);
        }

        return message;
    }

    public static String translateHexColorCodes(String message) {
        if (message == null) {
            return null;
        }

        if (isNotSupported()) {
            return message;
        }

        message = translateHexPattern(message);
        message = translateGradientCodes(message);
        message = translateSingleMessageColorCodes(message);

        return message;
    }

    public static String translateSingleMessageColorCodes(String message) {
        if (message == null) {
            return null;
        }

        StringBuilder result = new StringBuilder();
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

                    result.append(rgbColor.toString());
                    i += 7;
                    continue;
                }
            }
            result.append(message.charAt(i));
        }

        return result.toString();
    }

    public static String translateGradientCodes(String message) {
        if (message == null) {
            return null;
        }

        Matcher matcher = GRADIENT_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String startColor = matcher.group(1);
            String text = matcher.group(2);
            String endColor = matcher.group(3);
            
            String gradientText = createGradient(text, startColor, endColor);
            matcher.appendReplacement(buffer, gradientText);
        }

        return matcher.appendTail(buffer).toString();
    }

    public static String translateHexPattern(String message) {
        if (message == null) {
            return null;
        }

        Matcher matcher = HEX_PATTERN.matcher(message);
        StringBuffer buffer = new StringBuffer();

        while (matcher.find()) {
            String hexColor = matcher.group(1);
            String replacement = "§x";
            for (char c : hexColor.toCharArray()) {
                replacement += "§" + c;
            }
            matcher.appendReplacement(buffer, replacement);
        }

        return matcher.appendTail(buffer).toString();
    }

    private static String createGradient(String text, String startColor, String endColor) {
        if (text.isEmpty()) {
            return text;
        }

        StringBuilder result = new StringBuilder();
        int length = text.length();
        
        for (int i = 0; i < length; i++) {
            double ratio = (double) i / (length - 1);
            String interpolatedColor = interpolateColor(startColor, endColor, ratio);
            result.append("§x");
            for (char c : interpolatedColor.toCharArray()) {
                result.append("§").append(c);
            }
            result.append(text.charAt(i));
        }

        return result.toString();
    }

    private static String interpolateColor(String startColor, String endColor, double ratio) {
        int startR = Integer.parseInt(startColor.substring(0, 2), 16);
        int startG = Integer.parseInt(startColor.substring(2, 4), 16);
        int startB = Integer.parseInt(startColor.substring(4, 6), 16);
        
        int endR = Integer.parseInt(endColor.substring(0, 2), 16);
        int endG = Integer.parseInt(endColor.substring(2, 4), 16);
        int endB = Integer.parseInt(endColor.substring(4, 6), 16);
        
        int r = (int) (startR + (endR - startR) * ratio);
        int g = (int) (startG + (endG - startG) * ratio);
        int b = (int) (startB + (endB - startB) * ratio);
        
        return String.format("%02x%02x%02x", r, g, b);
    }

    private static boolean isNotSupported() {
        if (supported == null) {
            try {
                String version = Bukkit.getVersion();
                if (version.contains("MC: ")) {
                    String ver = version.split("\\(MC: ")[1];
                    String[] numbers = ver.replaceAll("\\)", "").split("\\.");
                    if (numbers.length >= 2) {
                        String major = numbers[0];
                        String minor = numbers[1];
                        int majorVersion = Integer.parseInt(major);
                        int minorVersion = Integer.parseInt(minor);
                        supported = majorVersion > 1 || (majorVersion == 1 && minorVersion >= 16);
                    } else {
                        supported = false;
                    }
                } else {
                    supported = false;
                }
            } catch (Exception e) {
                supported = false;
            }
        }

        return !supported;
    }
} 