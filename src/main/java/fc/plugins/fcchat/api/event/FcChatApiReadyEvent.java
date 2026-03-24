package fc.plugins.fcchat.api.event;

import fc.plugins.fcchat.api.FcChatApi;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class FcChatApiReadyEvent extends Event {
    private static final HandlerList HANDLERS = new HandlerList();
    private final FcChatApi api;

    public FcChatApiReadyEvent(FcChatApi api, boolean async) {
        super(async);
        this.api = api;
    }

    public FcChatApi getApi() {
        return api;
    }

    @Override
    public HandlerList getHandlers() {
        return HANDLERS;
    }

    public static HandlerList getHandlerList() {
        return HANDLERS;
    }
}
