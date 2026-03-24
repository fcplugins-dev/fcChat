package fc.plugins.fcchat.chat;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.utils.version.ToastManager;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class PingManager {
    private final ConfigManager configManager;
    private final SoundManager soundManager;

    public PingManager(ConfigManager configManager, SoundManager soundManager) {
        this.configManager = configManager;
        this.soundManager = soundManager;
    }

    public PingResult processPings(String message, Player sender) {
        if (!this.configManager.isPingSystemEnabled()) {
            return new PingResult(message, new ArrayList<Player>(), false);
        }
        ArrayList<Player> pingedPlayers = new ArrayList<Player>();
        boolean hasEveryonePing = false;
        String processedMessage = message;
        if (!sender.hasPermission(this.configManager.getPingPermission())) {
            return new PingResult(processedMessage, pingedPlayers, hasEveryonePing);
        }
        String symbol = this.configManager.getPingSymbol();
        String escapedSymbol = Pattern.quote(symbol);
        Pattern everyonePattern = Pattern.compile(escapedSymbol + "everyone", Pattern.CASE_INSENSITIVE);
        Pattern pingPattern = Pattern.compile(escapedSymbol + "(\\w+)");
        Matcher everyoneMatcher = everyonePattern.matcher(processedMessage);
        if (everyoneMatcher.find() && sender.hasPermission(this.configManager.getEveryonePingPermission())) {
            hasEveryonePing = true;
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                if (!onlinePlayer.equals(sender)) {
                    pingedPlayers.add(onlinePlayer);
                }
            }
            processedMessage = everyoneMatcher.replaceAll(this.configManager.getEveryonePingColor() + symbol + "everyone");
        }
        Matcher pingMatcher = pingPattern.matcher(processedMessage);
        while (pingMatcher.find()) {
            String playerName = pingMatcher.group(1);
            if (playerName.equalsIgnoreCase("everyone")) continue;
            Player targetPlayer = Bukkit.getPlayer(playerName);
            if (targetPlayer == null || !targetPlayer.isOnline() || targetPlayer.equals(sender)) continue;
            if (!pingedPlayers.contains(targetPlayer)) {
                pingedPlayers.add(targetPlayer);
            }
            String pingColor = this.configManager.getPingColor();
            processedMessage = processedMessage.replaceFirst(escapedSymbol + Pattern.quote(playerName), pingColor + symbol + playerName);
        }
        return new PingResult(processedMessage, pingedPlayers, hasEveryonePing);
    }

    public void playPingSounds(List<Player> pingedPlayers, boolean hasEveryonePing) {
        if (!this.configManager.isPingSystemEnabled()) {
            return;
        }
        for (Player player : pingedPlayers) {
            if (hasEveryonePing) {
                this.soundManager.playEveryonePingSound(player);
            } else {
                this.soundManager.playPingSound(player);
            }
            if (this.configManager.isPingToastEnabled()) {
                sendPingToast(player);
            }
        }
    }

    public void sendPingToast(Player player) {
        if (!ToastManager.isSupported()) {
            return;
        }
        String toastText = this.configManager.getPingToastText();
        ToastManager.sendToast(player, toastText);
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
            return this.processedMessage;
        }

        public List<Player> getPingedPlayers() {
            return this.pingedPlayers;
        }

        public boolean hasEveryonePing() {
            return this.hasEveryonePing;
        }
    }
}
