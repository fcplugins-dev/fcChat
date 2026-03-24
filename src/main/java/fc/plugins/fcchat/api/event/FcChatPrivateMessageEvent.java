package fc.plugins.fcchat.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FcChatPrivateMessageEvent extends Event implements Cancellable {
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
        return sender;
    }

    public Player getReceiver() {
        return receiver;
    }

    public String getMessage() {
        return message;
    }

    public String getSenderFormat() {
        return senderFormat;
    }

    public void setSenderFormat(String senderFormat) {
        this.senderFormat = senderFormat;
    }

    public String getReceiverFormat() {
        return receiverFormat;
    }

    public void setReceiverFormat(String receiverFormat) {
        this.receiverFormat = receiverFormat;
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
