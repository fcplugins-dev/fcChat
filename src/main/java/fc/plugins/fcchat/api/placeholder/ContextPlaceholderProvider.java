package fc.plugins.fcchat.api.placeholder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface ContextPlaceholderProvider {
    String resolve(CommandSender sender, Player viewer, String rawMessage);
}
