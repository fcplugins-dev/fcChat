package fc.plugins.fcchat.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PrivateMessageManager {
    private final Map<UUID, UUID> lastMessengers;

    public PrivateMessageManager() {
        this.lastMessengers = new HashMap<>();
    }

    public void setLastMessenger(UUID player, UUID messenger) {
        this.lastMessengers.put(player, messenger);
    }

    public UUID getLastMessenger(UUID player) {
        return this.lastMessengers.get(player);
    }

}
