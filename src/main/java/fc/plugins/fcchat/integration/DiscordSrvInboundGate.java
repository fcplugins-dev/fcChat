package fc.plugins.fcchat.integration;

import fc.plugins.fcchat.FcChat;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.DiscordGuildMessagePreBroadcastEvent;

public class DiscordSrvInboundGate {
    private final FcChat plugin;

    public DiscordSrvInboundGate(FcChat plugin) {
        this.plugin = plugin;
    }

    @Subscribe
    public void onDiscordPreBroadcast(DiscordGuildMessagePreBroadcastEvent event) {
        if (!this.plugin.getConfig().getBoolean("chat.enabled", true)) {
            event.getRecipients().clear();
        }
    }
}
