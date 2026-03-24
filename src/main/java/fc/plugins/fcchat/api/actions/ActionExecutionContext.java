package fc.plugins.fcchat.api.actions;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.Map;

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
        return sender;
    }

    public Player getTarget() {
        return target;
    }

    public String getMessage() {
        return message;
    }

    public Map<String, String> getMetadata() {
        return metadata;
    }
}
