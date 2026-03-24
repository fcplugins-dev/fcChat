package fc.plugins.fcchat.utils;

import fc.plugins.fcchat.FcChat;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import org.bukkit.entity.Player;
import org.json.JSONException;
import org.json.JSONObject;

public class Updater {
    private final FcChat plugin;
    private boolean isHasNewerVersion;
    private String latestVersion;
    private String currentVersion;

    public Updater(FcChat plugin) {
        this.plugin = plugin;
        this.latestVersion = plugin.getConfigManager().updateCheck() ? this.getLatestVersionFromSpigot() : "v2.2";
        this.currentVersion = plugin.getDescription().getVersion();
        if (this.isNewerVersion(this.latestVersion, this.currentVersion)) {
            this.isHasNewerVersion = true;
            plugin.getLogger().info("§6§l[fcChat] §fДоступна новая §6версия §fплагина§6: " + this.latestVersion);
            plugin.getLogger().info("§6https://www.spigotmc.org/resources/fcchat-advanced-chat-management-plugin.127544");
        } else {
            this.isHasNewerVersion = false;
            plugin.getLogger().info("§a[✔] §fВы используете §aпоследнюю §fверсию плагина§a!");
        }
    }

    private boolean isNewerVersion(String latestVersion, String currentVersion) {
        if (latestVersion == null) {
            return false;
        }
        String latest = latestVersion.split("-")[0];
        String current = currentVersion.split("-")[0];
        String[] latestParts = latest.split("\\.");
        String[] currentParts = current.split("\\.");
        int i = 0;
        while (i < Math.max(latestParts.length, currentParts.length)) {
            int latestPart = 0;
            int currentPart = 0;
            try {
                String latestStr;
                String string = latestStr = i < latestParts.length ? latestParts[i] : "0";
                if (latestStr.startsWith("v")) {
                    latestStr = latestStr.substring(1);
                }
                latestPart = Integer.parseInt(latestStr);
            }
            catch (NumberFormatException e) {
                latestPart = 0;
            }
            try {
                String currentStr;
                String string = currentStr = i < currentParts.length ? currentParts[i] : "0";
                if (currentStr.startsWith("v")) {
                    currentStr = currentStr.substring(1);
                }
                currentPart = Integer.parseInt(currentStr);
            }
            catch (NumberFormatException e) {
                currentPart = 0;
            }
            if (latestPart > currentPart) {
                return true;
            }
            if (latestPart < currentPart) {
                return false;
            }
            ++i;
        }
        return false;
    }

    public void sendUpdateMessageToPlayer(Player player) {
        if (!this.plugin.getConfigManager().updateCheck()) {
            return;
        }
        if (this.isHasNewerVersion) {
            player.sendMessage("§6§l[fcChat] §fДоступна новая §6версия §fплагина§6: " + this.latestVersion);
            player.sendMessage("§6https://www.spigotmc.org/resources/fcchat-advanced-chat-management-plugin.127544");
        } else {
            player.sendMessage("§a§l[fcChat] §fВы используете §aпоследнюю §fверсию плагина§a!");
        }
    }

    private String getLatestVersionFromSpigot() {
        try {
            String line;
            URL url = new URL("https://api.spigotmc.org/simple/0.1/index.php?action=getResource&id=127544");
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            StringBuilder response = new StringBuilder();
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            reader.close();
            JSONObject jsonResponse = new JSONObject(response.toString());
            return jsonResponse.getString("current_version");
        }
        catch (IOException | JSONException var7) {
            return null;
        }
    }
}
