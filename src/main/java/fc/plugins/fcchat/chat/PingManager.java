package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.config.ConfigManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PingManager {
    private final ConfigManager configManager;
    private final SoundManager soundManager;
    private static final Pattern PING_PATTERN = Pattern.compile("@(\\w+)");
    private static final Pattern EVERYONE_PATTERN = Pattern.compile("@everyone", Pattern.CASE_INSENSITIVE);

    public PingManager(ConfigManager configManager, SoundManager soundManager) {
        this.configManager = configManager;
        this.soundManager = soundManager;
    }

    public PingResult processPings(String message, Player sender) {
        if (!configManager.isPingSystemEnabled()) {
            return new PingResult(message, new ArrayList<>(), false);
        }

        List<Player> pingedPlayers = new ArrayList<>();
        boolean hasEveryonePing = false;
        String processedMessage = message;

        if (!sender.hasPermission(configManager.getPingPermission())) {
            return new PingResult(processedMessage, pingedPlayers, hasEveryonePing);
        }

        Matcher everyoneMatcher = EVERYONE_PATTERN.matcher(processedMessage);
        if (everyoneMatcher.find()) {
            hasEveryonePing = true;
            if (sender.hasPermission(configManager.getEveryonePingPermission())) {
                pingedPlayers.addAll(Bukkit.getOnlinePlayers());
                processedMessage = everyoneMatcher.replaceAll(configManager.getEveryonePingColor() + "@everyone");
            }
        }

        Matcher pingMatcher = PING_PATTERN.matcher(processedMessage);
        while (pingMatcher.find()) {
            String playerName = pingMatcher.group(1);
            Player targetPlayer = Bukkit.getPlayer(playerName);
            
            if (targetPlayer != null && targetPlayer.isOnline()) {
                pingedPlayers.add(targetPlayer);
                String pingColor = configManager.getPingColor();
                processedMessage = processedMessage.replaceFirst("@" + playerName, pingColor + "@" + playerName);
            }
        }

        return new PingResult(processedMessage, pingedPlayers, hasEveryonePing);
    }

    public void playPingSounds(List<Player> pingedPlayers, boolean hasEveryonePing) {
        if (!configManager.isPingSystemEnabled()) {
            return;
        }

        for (Player player : pingedPlayers) {
            if (hasEveryonePing && player.hasPermission(configManager.getEveryonePingPermission())) {
                soundManager.playEveryonePingSound(player);
            } else {
                soundManager.playPingSound(player);
            }
        }
    }

    public static class PingResult {
        private final String processedMessage;
        private final List<Player> pingedPlayers;
        private final boolean hasEveryonePing;

        public PingResult(String processedMessage, List<Player> pingedPlayers, boolean hasEveryonePing) {
            this.processedMessage = processedMessage;
            this.pingedPlayers = pingedPlayers;
            this.hasEveryonePing = hasEveryonePing;
        }

        public String getProcessedMessage() {
            return processedMessage;
        }

        public List<Player> getPingedPlayers() {
            return pingedPlayers;
        }

        public boolean hasEveryonePing() {
            return hasEveryonePing;
        }
    }
}
