package fc.plugins.fcchat.commands;

import fc.plugins.fcchat.config.ConfigManager;
import fc.plugins.fcchat.FcChat;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ChatTabCompleter implements TabCompleter {
    private final ConfigManager configManager;
    private final FcChat plugin;

    public ChatTabCompleter(ConfigManager configManager, FcChat plugin) {
        this.configManager = configManager;
        this.plugin = plugin;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("fcchat")) {
            if (args.length == 1) {
                if (sender.hasPermission("fcchat.reload")) {
                    completions.add("reload");
                }
                if (sender.hasPermission(configManager.getSpyPermission())) {
                    completions.add("spy");
                }
                completions.add("channel");
                if (sender.hasPermission("fcchat.clear")) {
                    completions.add("clear");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("spy")) {
                if (sender.hasPermission(configManager.getSpyPermission())) {
                    completions.add("on");
                    completions.add("off");
                }
            } else if (args.length == 2 && args[0].equalsIgnoreCase("channel")) {
                completions.add("default");
                Map<String, fc.plugins.fcchat.channel.Channel> channels = plugin.getChatManager().getChannelManager().getAllChannels();
                for (Map.Entry<String, fc.plugins.fcchat.channel.Channel> entry : channels.entrySet()) {
                    fc.plugins.fcchat.channel.Channel channel = entry.getValue();
                    if (channel.isEnabled() && sender.hasPermission(channel.getPermission())) {
                        completions.add(entry.getKey());
                    }
                }
            }
        }
        
        return completions;
    }
} 