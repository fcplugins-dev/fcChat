package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.chat.channel.Channel;
import fc.plugins.fcchat.manager.config.ConfigManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

public class ChatTabCompleter
        implements TabCompleter {
    private final ConfigManager configManager;
    private final FcChat plugin;

    public ChatTabCompleter(ConfigManager configManager, FcChat plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }

    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        ArrayList<String> completions = new ArrayList<String>();
        if (command.getName().equalsIgnoreCase("fcchat")) {
            if (args.length == 1) {
                if (sender.hasPermission("fcchat.reload")) {
                    completions.add("reload");
                }
                if (sender.hasPermission("fcchat.ai.manage")) {
                    completions.add("ai");
                }
                completions.add("channel");
                if (sender.hasPermission("fcchat.clear")) {
                    completions.add("clear");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("ai") && sender.hasPermission("fcchat.ai.manage")) {
                completions.add("stats");
                completions.add("logger");
            } else if (args.length == 2 && args[0].equalsIgnoreCase("channel")) {
                completions.add("default");
                Map<String, Channel> channels = this.plugin.getChatManager().getChannelManager().getAllChannels();
                for (Map.Entry<String, Channel> entry : channels.entrySet()) {
                    Channel channel = entry.getValue();
                    if (!channel.isEnabled()) continue;
                    if (channel.isClanChannel()) {
                        if (!(sender instanceof Player)) continue;
                        Player player = (Player)((Object)sender);
                        if (!this.plugin.getChatManager().getChannelManager().hasChannelPermission(player, entry.getKey())) continue;
                        completions.add(entry.getKey());
                        continue;
                    }
                    if (!sender.hasPermission(channel.getPermission())) continue;
                    completions.add(entry.getKey());
                }
            }
        }
        return completions;
    }
}
