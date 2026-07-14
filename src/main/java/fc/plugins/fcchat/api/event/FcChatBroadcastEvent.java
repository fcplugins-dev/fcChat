
package fc.plugins.fcchat.api.event;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FcChatBroadcastEvent
extends Event
implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final CommandSender sender;
    private final String message;
    private String formattedMessage;
    private Set<Player> recipients;
    private boolean cancelled;

    public FcChatBroadcastEvent(CommandSender sender, String message, String formattedMessage, Set<Player> recipients, boolean async) {
        super(async);
        this.sender = sender;
        this.message = message;
        this.formattedMessage = formattedMessage;
        this.recipients = new HashSet<Player>(recipients);
    }

    public CommandSender getSender() {
        return this.sender;
    }

    public String getMessage() {
        return this.message;
    }

    public String getFormattedMessage() {
        return this.formattedMessage;
    }

    public void setFormattedMessage(String formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public Set<Player> getRecipients() {
        return Collections.unmodifiableSet(this.recipients);
    }

    public void setRecipients(Set<Player> recipients) {
        this.recipients = new HashSet<Player>(recipients);
    }

    public boolean isCancelled() {
        return this.cancelled;
    }

    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}

