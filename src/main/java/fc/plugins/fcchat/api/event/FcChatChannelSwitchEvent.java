
package fc.plugins.fcchat.api.event;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FcChatChannelSwitchEvent
extends Event
implements Cancellable {
    private static final HandlerList HANDLERS = new HandlerList();
    private final Player player;
    private final String oldChannel;
    private final String newChannel;
    private boolean cancelled;

    public FcChatChannelSwitchEvent(Player player, String oldChannel, String newChannel, boolean async) {
        super(async);
        this.player = player;
        this.oldChannel = oldChannel;
        this.newChannel = newChannel;
    }

    public Player getPlayer() {
        return this.player;
    }

    public String getOldChannel() {
        return this.oldChannel;
    }

    public String getNewChannel() {
        return this.newChannel;
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

