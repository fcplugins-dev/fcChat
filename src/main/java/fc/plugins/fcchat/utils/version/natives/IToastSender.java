package fc.plugins.fcchat.utils.version.natives;

import org.bukkit.entity.Player;

public interface IToastSender {
    void sendToast(Player player, String text);
}
