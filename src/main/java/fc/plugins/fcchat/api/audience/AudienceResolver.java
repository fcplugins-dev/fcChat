package fc.plugins.fcchat.api.audience;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Set;

public interface AudienceResolver {
    String getId();

    int getPriority();

    Set<Player> resolveRecipients(CommandSender sender, String message, Set<Player> currentRecipients);
}
