
package fc.plugins.fcchat.api.placeholder;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public interface ContextPlaceholderProvider {
    public String resolve(CommandSender var1, Player var2, String var3);
}

