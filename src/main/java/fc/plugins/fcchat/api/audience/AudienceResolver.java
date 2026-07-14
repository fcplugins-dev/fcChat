
package fc.plugins.fcchat.api.audience;

import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface AudienceResolver {
    public String getId();

    public int getPriority();

    public Set<Player> resolveRecipients(CommandSender var1, String var2, Set<Player> var3);
}

