package fc.plugins.fcchat.chat.automessages;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.utils.HexUtils;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import net.md_5.bungee.api.chat.*;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class AutoMessages {
    private final FcChat plugin;
    private FileConfiguration config;
    private File configFile;
    private int taskId;
    private final Random random = new Random();

    public AutoMessages(FcChat plugin) {
        this.plugin = plugin;
        this.loadConfig();
    }

    private void loadConfig() {
        this.configFile = new File(this.plugin.getDataFolder(), "automessages.yml");
        if (!this.configFile.exists()) {
            this.plugin.saveResource("automessages.yml", false);
        }
        this.config = YamlConfiguration.loadConfiguration(this.configFile);
        if (!this.config.contains("current_index")) {
            this.config.set("current_index", 1);
            try {
                this.config.save(this.configFile);
            } catch (Exception e) {}
        }
    }

    public void start() {
        if (!this.isEnabled()) {
            return;
        }
        int interval = this.getInterval() * 20;
        this.taskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(this.plugin, this::sendMessage, interval, interval);
    }

    public void stop() {
        if (this.taskId != 0) {
            Bukkit.getScheduler().cancelTask(this.taskId);
            this.taskId = 0;
        }
    }

    public void reload() {
        this.stop();
        this.loadConfig();
        this.start();
    }

    private void sendMessage() {
        if (!this.isEnabled()) {
            return;
        }
        String mode = this.getMode();
        MessageGroup messageGroup = null;
        messageGroup = mode.equals("random") ? this.getRandomMessageGroup() : this.getNextMessageGroup();
        if (messageGroup != null) {
            this.sendMessageGroup(messageGroup);
        }
    }

    private MessageGroup getRandomMessageGroup() {
        List<MessageGroup> allGroups = this.getAllMessageGroups();
        if (!allGroups.isEmpty()) {
            return allGroups.get(this.random.nextInt(allGroups.size()));
        }
        return null;
    }

    private MessageGroup getNextMessageGroup() {
        int currentIndex = this.config.getInt("current_index");
        String key = "messages_" + currentIndex;
        MessageGroup group = this.getMessageGroup(key);
        int nextIndex = currentIndex + 1;
        int maxIndex = this.getMaxMessageIndex();
        if (nextIndex > maxIndex) {
            nextIndex = 1;
        }
        this.config.set("current_index", nextIndex);
        try {
            this.config.save(this.configFile);
        }
        catch (Exception exception) {
        }
        return group;
    }

    private List<MessageGroup> getAllMessageGroups() {
        ArrayList<MessageGroup> groups = new ArrayList<MessageGroup>();
        for (String key : this.config.getKeys(false)) {
            MessageGroup group;
            if (!key.startsWith("messages_") || (group = this.getMessageGroup(key)) == null) continue;
            groups.add(group);
        }
        return groups;
    }

    private MessageGroup getMessageGroup(String key) {
        if (!this.config.contains(key)) {
            return null;
        }
        List messages = this.config.getStringList(key + ".messages");
        String sound = this.config.getString(key + ".sound");
        if (messages.isEmpty()) {
            return null;
        }
        return new MessageGroup(messages, sound);
    }

    private int getMaxMessageIndex() {
        int maxIndex = 0;
        for (String key : this.config.getKeys(false)) {
            if (!key.startsWith("messages_")) continue;
            try {
                int index = Integer.parseInt(key.substring(9));
                maxIndex = Math.max(maxIndex, index);
            }
            catch (NumberFormatException numberFormatException) {}
        }
        return maxIndex;
    }

    private void sendMessageGroup(MessageGroup group) {
        for (String message : group.getMessages()) {
            TextComponent component = this.parseMessage(message);
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.spigot().sendMessage(component);
            }
        }
        try {
            Sound sound = Sound.valueOf(group.getSound());
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.playSound(player.getLocation(), sound, 0.5f, 1.0f);
            }
        }
        catch (IllegalArgumentException illegalArgumentException) {
        }
    }

    private TextComponent parseMessage(String message) {
        String translatedMessage;
        if (message.contains("{") && message.contains("}")) {
            int startIndex = message.indexOf("{");
            int endIndex = message.indexOf("}");
            if (startIndex != -1 && endIndex != -1 && endIndex > startIndex) {
                String translatedAfter;
                String translatedText;
                String translatedBefore;
                String text;
                String url;
                String hoverText = null;
                String before = message.substring(0, startIndex);
                String linkText = message.substring(startIndex + 1, endIndex);
                String after = message.substring(endIndex + 1);
                
                if (linkText.startsWith("http://") || linkText.startsWith("https://")) {
                    int firstColon = linkText.indexOf(":", 8);
                    if (firstColon != -1) {
                        url = linkText.substring(0, firstColon);
                        String remaining = linkText.substring(firstColon + 1);
                        int secondColon = remaining.indexOf(":");
                        if (secondColon != -1) {
                            text = remaining.substring(0, secondColon);
                            hoverText = remaining.substring(secondColon + 1);
                        } else {
                            text = remaining;
                        }
                    } else {
                        url = linkText;
                        text = linkText;
                    }
                } else {
                    String[] parts = linkText.split(":", 3);
                    if (parts.length >= 2) {
                        url = parts[0];
                        text = parts[1];
                        if (parts.length >= 3) {
                            hoverText = parts[2];
                        }
                    } else {
                        url = linkText;
                        text = linkText;
                    }
                }
                
                TextComponent component = new TextComponent();
                if (!before.isEmpty() && (translatedBefore = HexUtils.translateAlternateColorCodes(before)) != null) {
                    BaseComponent[] beforeComponents = TextComponent.fromLegacyText(translatedBefore);
                    component.addExtra(new TextComponent(beforeComponents));
                }
                if ((translatedText = HexUtils.translateAlternateColorCodes(text)) != null) {
                    BaseComponent[] textComponents = TextComponent.fromLegacyText(translatedText);
                    TextComponent clickableText = new TextComponent(textComponents);
                    if (url.startsWith("http")) {
                        clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url));
                    } else {
                        clickableText.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, url));
                    }
                    if (hoverText != null && !hoverText.isEmpty()) {
                        String translatedHoverText = HexUtils.translateAlternateColorCodes(hoverText);
                        HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(translatedHoverText).create());
                        clickableText.setHoverEvent(hoverEvent);
                    }
                    component.addExtra(clickableText);
                }
                if (!after.isEmpty() && (translatedAfter = HexUtils.translateAlternateColorCodes(after)) != null) {
                    BaseComponent[] afterComponents = TextComponent.fromLegacyText(translatedAfter);
                    component.addExtra(new TextComponent(afterComponents));
                }
                return component;
            }
        }
        if ((translatedMessage = HexUtils.translateAlternateColorCodes(message)) != null) {
            BaseComponent[] components = TextComponent.fromLegacyText(translatedMessage);
            return new TextComponent(components);
        }
        BaseComponent[] components = TextComponent.fromLegacyText(message);
        return new TextComponent(components);
    }

    public boolean isEnabled() {
        return this.config.getBoolean("automessages");
    }

    public int getInterval() {
        return this.config.getInt("interval");
    }

    public String getMode() {
        return this.config.getString("mode");
    }
}
