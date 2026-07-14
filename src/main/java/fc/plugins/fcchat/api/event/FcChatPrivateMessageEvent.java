
package fc.plugins.fcchat.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FcChatPrivateMessageEvent
extends Event
implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player sender;
    private final Player receiver;
    private final String message;
    private String senderFormat;
    private String receiverFormat;
    private boolean cancelled;

    public FcChatPrivateMessageEvent(Player sender, Player receiver, String message, String senderFormat, String receiverFormat, boolean async) {
        super(async);
        this.sender = sender;
        this.receiver = receiver;
        this.message = message;
        this.senderFormat = senderFormat;
        this.receiverFormat = receiverFormat;
    }

    public Player getSender() {
        return this.sender;
    }

    public Player getReceiver() {
        return this.receiver;
    }

    public String getMessage() {
        return this.message;
    }

    public String getSenderFormat() {
        return this.senderFormat;
    }

    public void setSenderFormat(String senderFormat) {
        this.senderFormat = senderFormat;
    }

    public String getReceiverFormat() {
        return this.receiverFormat;
    }

    public void setReceiverFormat(String receiverFormat) {
        this.receiverFormat = receiverFormat;
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

