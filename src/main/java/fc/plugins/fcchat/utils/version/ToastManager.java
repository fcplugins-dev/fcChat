package fc.plugins.fcchat.utils.version;

import fc.plugins.fcchat.utils.version.natives.IToastSender;
import fc.plugins.fcchat.utils.version.natives.Toast_1_16;
import fc.plugins.fcchat.utils.version.natives.Toast_1_21;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ToastManager {
    private static IToastSender toastSender;
    private static boolean isSupported = false;
    
    static {
        try {
            String packageName = Bukkit.getServer().getClass().getPackage().getName();
            String[] parts = packageName.split("\\.");
            String bukkitVersion = Bukkit.getBukkitVersion();
            boolean isModern21 = isAtLeast121(bukkitVersion);
            
            if (parts.length >= 4) {
                String serverVersion = parts[3];
                
                if (serverVersion.startsWith("v1_16") || serverVersion.startsWith("v1_17") || 
                    serverVersion.startsWith("v1_18") || serverVersion.startsWith("v1_19") || 
                    serverVersion.startsWith("v1_20")) {
                    toastSender = new Toast_1_16(serverVersion);
                    isSupported = true;
                } else if (serverVersion.startsWith("v1_21") || isModern21) {
                    toastSender = new Toast_1_21();
                    isSupported = true;
                }
            } else if (isModern21) {
                toastSender = new Toast_1_21();
                isSupported = true;
            }
        } catch (Exception e) {
            isSupported = false;
        }
    }
    
    public static boolean isSupported() {
        return isSupported;
    }
    
    public static void sendToast(Player player, String text) {
        if (!isSupported || toastSender == null) {
            return;
        }
        
        try {
            toastSender.sendToast(player, text);
        } catch (Exception e) {
        }
    }

    private static boolean isAtLeast121(String bukkitVersion) {
        if (bukkitVersion == null || bukkitVersion.isEmpty()) {
            return false;
        }

        try {
            String[] versionParts = bukkitVersion.split("-")[0].split("\\.");
            int major = Integer.parseInt(versionParts[0]);
            int minor = Integer.parseInt(versionParts[1]);

            if (major > 1) {
                return true;
            }
            return major == 1 && minor >= 21;
        } catch (Exception ignored) {
            return false;
        }
    }
}
