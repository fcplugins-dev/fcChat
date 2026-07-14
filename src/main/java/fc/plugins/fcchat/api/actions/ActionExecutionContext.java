
package fc.plugins.fcchat.api.actions;

import java.util.Collections;
import java.util.Map;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ActionExecutionContext {
    private final CommandSender sender;
    private final Player target;
    private final String message;
    private final Map<String, String> metadata;

    public ActionExecutionContext(CommandSender sender, Player target, String message, Map<String, String> metadata) {
        this.sender = sender;
        this.target = target;
        this.message = message;
        this.metadata = metadata == null ? Collections.emptyMap() : Collections.unmodifiableMap(metadata);
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public Player getTarget() {
        return this.target;
    }

    public String getMessage() {
        return this.message;
    }

    public Map<String, String> getMetadata() {
        return this.metadata;
    }
}

