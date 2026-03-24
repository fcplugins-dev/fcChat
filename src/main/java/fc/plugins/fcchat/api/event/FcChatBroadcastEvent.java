package fc.plugins.fcchat.api.event;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class FcChatBroadcastEvent extends Event implements Cancellable {
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
        this.recipients = new HashSet<>(recipients);
    }

    public CommandSender getSender() {
        return sender;
    }

    public String getMessage() {
        return message;
    }

    public String getFormattedMessage() {
        return formattedMessage;
    }

    public void setFormattedMessage(String formattedMessage) {
        this.formattedMessage = formattedMessage;
    }

    public Set<Player> getRecipients() {
        return Collections.unmodifiableSet(recipients);
    }

    public void setRecipients(Set<Player> recipients) {
        this.recipients = new HashSet<>(recipients);
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancelled = cancel;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
