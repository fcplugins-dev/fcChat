package fc.plugins.fcchat.automessages;

import fc.plugins.fcchat.FcChat;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

class MessageGroup {
    private final List<String> messages;
    private final String sound;

    public MessageGroup(List<String> messages, String sound) {
        this.messages = messages;
        this.sound = sound;
    }

    public List<String> getMessages() {
        return messages;
    }

    public String getSound() {
        return sound;
    }
}

public class AutoMessages {
    private final FcChat plugin;
    private FileConfiguration config;
    private File configFile;
    private int taskId;
    private final Random random = new Random();

    public AutoMessages(FcChat plugin) {
        this.plugin = plugin;
        loadConfig();
    }

    private void loadConfig() {
        configFile = new File(plugin.getDataFolder(), "automessages.yml");
        if (!configFile.exists()) {
            plugin.saveResource("automessages.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
    }

    public void start() {
        if (!isEnabled()) {
            return;
        }

        int interval = getInterval() * 20;
        taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, this::sendMessage, interval, interval);
    }

    public void stop() {
        if (taskId != 0) {
            Bukkit.getScheduler().cancelTask(taskId);
            taskId = 0;
        }
    }

    public void reload() {
        stop();
        loadConfig();
        start();
    }

    private void sendMessage() {
        if (!isEnabled()) {
            return;
        }

        String mode = getMode();
        MessageGroup messageGroup = null;

        if (mode.equals("random")) {
            messageGroup = getRandomMessageGroup();
        } else {
            messageGroup = getNextMessageGroup();
        }

        if (messageGroup != null) {
            sendMessageGroup(messageGroup);
        }
    }

    private MessageGroup getRandomMessageGroup() {
        List<MessageGroup> allGroups = getAllMessageGroups();
        if (!allGroups.isEmpty()) {
            return allGroups.get(random.nextInt(allGroups.size()));
        }
        return null;
    }

    private MessageGroup getNextMessageGroup() {
        int currentIndex = config.getInt("current_index", 1);
        
        String key = "messages_" + currentIndex;
        MessageGroup group = getMessageGroup(key);
        
        int nextIndex = currentIndex + 1;
        int maxIndex = getMaxMessageIndex();
        if (nextIndex > maxIndex) {
            nextIndex = 1;
        }
        
        config.set("current_index", nextIndex);
        try {
            config.save(configFile);
        } catch (Exception e) {
        }

        return group;
    }

    private List<MessageGroup> getAllMessageGroups() {
        List<MessageGroup> groups = new ArrayList<>();
        for (String key : config.getKeys(false)) {
            if (key.startsWith("messages_")) {
                MessageGroup group = getMessageGroup(key);
                if (group != null) {
                    groups.add(group);
                }
            }
        }
        return groups;
    }

    private MessageGroup getMessageGroup(String key) {
        if (!config.contains(key)) {
            return null;
        }
        
        List<String> messages = config.getStringList(key + ".messages");
        String sound = config.getString(key + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
        
        if (messages.isEmpty()) {
            return null;
        }
        
        return new MessageGroup(messages, sound);
    }

    private int getMaxMessageIndex() {
        int maxIndex = 0;
        for (String key : config.getKeys(false)) {
            if (key.startsWith("messages_")) {
                try {
                    int index = Integer.parseInt(key.substring(9));
                    maxIndex = Math.max(maxIndex, index);
                } catch (NumberFormatException ignored) {}
            }
        }
        return maxIndex;
    }

    private void sendMessageGroup(MessageGroup group) {
        for (String message : group.getMessages()) {
            TextComponent component = parseMessage(message);
            
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(component);
            }
        }
        
        try {
            Sound sound = Sound.valueOf(group.getSound());
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, 0.5f, 1.0f);
            }
        } catch (IllegalArgumentException e) {
        }
    }

    private TextComponent parseMessage(String message) {
        if (message.contains("{") && message.contains("}")) {
            int startIndex = message.indexOf("{");
            int endIndex = message.indexOf("}");
            
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                String before = message.substring(0, startIndex);
                String linkText = message.substring(startIndex + 1, endIndex);
                String after = message.substring(endIndex + 1);
                
                int colonIndex = linkText.lastIndexOf(":");
                String url, text;
                
                if (colonIndex != -1 && colonIndex < linkText.length() - 1) {
                    url = linkText.substring(0, colonIndex);
                    text = linkText.substring(colonIndex + 1);
                } else {
                    url = linkText;
                    text = linkText;
                }
                
                TextComponent component = new TextComponent();
                
                if (!before.isEmpty()) {
                    component.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', before)));
                }
                
                TextComponent clickableText = new TextComponent(ChatColor.translateAlternateColorCodes('&', text));
                
                if (url.startsWith("http")) {
                    clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                } else {
                    clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, url));
                }
                
                component.addExtra(clickableText);
                
                if (!after.isEmpty()) {
                    component.addExtra(new TextComponent(ChatColor.translateAlternateColorCodes('&', after)));
                }
                
                return component;
            }
        }
        
        return new TextComponent(ChatColor.translateAlternateColorCodes('&', message));
    }

    public boolean isEnabled() {
        return config.getBoolean("automessages", true);
    }

    public int getInterval() {
        return config.getInt("interval", 300);
    }

    public String getMode() {
        return config.getString("mode", "random");
    }
} 