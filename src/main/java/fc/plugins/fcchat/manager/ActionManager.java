package fc.plugins.fcchat.manager;

import fc.plugins.fcchat.FcChat;
import fc.plugins.fcchat.utils.HexUtils;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.List;
import java.util.Set;

public class ActionManager {
    private final FcChat plugin;
    private FileConfiguration actionConfig;
    private File actionFile;
    
    public ActionManager(FcChat plugin) {
        this.plugin = plugin;
        this.loadActionConfig();
    }
    
    private void loadActionConfig() {
        this.actionFile = new File(this.plugin.getDataFolder(), "action.yml");
        if (!this.actionFile.exists()) {
            this.plugin.saveResource("action.yml", false);
        }
        this.actionConfig = YamlConfiguration.loadConfiguration(this.actionFile);
    }
    
    public void reloadActionConfig() {
        this.loadActionConfig();
    }
    
    public boolean isEnabled() {
        return this.actionConfig.getBoolean("enabled", true);
    }
    
    public TextComponent applyAction(TextComponent component, String originalMessage, Player sender, Player receiver, String target) {
        if (!this.isEnabled()) {
            return component;
        }
        
        ConfigurationSection actionsSection = this.actionConfig.getConfigurationSection("actions");
        if (actionsSection == null) {
            return component;
        }
        
        Set<String> actionKeys = actionsSection.getKeys(false);
        
        StringBuilder combinedHoverText = new StringBuilder();
        ClickEvent clickEvent = null;
        
        for (String actionKey : actionKeys) {
            String actionPath = "actions." + actionKey;
            
            if (!this.actionConfig.getBoolean(actionPath + ".enabled", false)) {
                continue;
            }
            
            String actionTarget = this.actionConfig.getString(actionPath + ".target", "message");
            if (!actionTarget.equalsIgnoreCase(target)) {
                continue;
            }
            
            String permission = this.actionConfig.getString(actionPath + ".permission", "");
            if (!permission.isEmpty() && !receiver.hasPermission(permission)) {
                continue;
            }
            
            List<String> commands = this.actionConfig.getStringList(actionPath + ".click-commands");
            if (commands.isEmpty()) {
                continue;
            }
            
            for (String command : commands) {
                String processedCommand = command.replace("{player}", sender.getName())
                                                .replace("{message}", originalMessage)
                                                .replace("{receiver}", receiver.getName());
                
                if (processedCommand.startsWith("[hover] ")) {
                    String hoverText = processedCommand.substring(8).trim();
                    if (combinedHoverText.length() > 0) {
                        combinedHoverText.append("\n");
                    }
                    combinedHoverText.append(hoverText);
                } else if (clickEvent == null) {
                    if (processedCommand.startsWith("[copy] ")) {
                        String text = processedCommand.substring(7).trim();
                        clickEvent = new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, text);
                    } else if (processedCommand.startsWith("[paste] ")) {
                        String text = processedCommand.substring(8).trim();
                        clickEvent = new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, text);
                    } else if (processedCommand.startsWith("[command] ")) {
                        String cmd = processedCommand.substring(10).trim();
                        clickEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, cmd);
                    } else if (processedCommand.startsWith("[url] ")) {
                        String url = processedCommand.substring(6).trim();
                        clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, url);
                    }
                }
            }
        }
        
        if (combinedHoverText.length() > 0) {
            String coloredHoverText = HexUtils.translateAlternateColorCodes(combinedHoverText.toString());
            HoverEvent hoverEvent = new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(coloredHoverText).create());
            component.setHoverEvent(hoverEvent);
        }
        
        if (clickEvent != null) {
            component.setClickEvent(clickEvent);
        }
        
        return component;
    }
    
    public void executeClickCommands(Player player, String originalMessage, Player sender, String target) {
        if (!this.isEnabled()) {
            return;
        }
        
        ConfigurationSection actionsSection = this.actionConfig.getConfigurationSection("actions");
        if (actionsSection == null) {
            return;
        }
        
        Set<String> actionKeys = actionsSection.getKeys(false);
        
        for (String actionKey : actionKeys) {
            String actionPath = "actions." + actionKey;
            
            if (!this.actionConfig.getBoolean(actionPath + ".enabled", false)) {
                continue;
            }
            
            String actionTarget = this.actionConfig.getString(actionPath + ".target", "message");
            if (!actionTarget.equalsIgnoreCase(target)) {
                continue;
            }
            
            String permission = this.actionConfig.getString(actionPath + ".permission", "");
            if (!permission.isEmpty() && !player.hasPermission(permission)) {
                continue;
            }
            
            List<String> commands = this.actionConfig.getStringList(actionPath + ".click-commands");
            if (commands.isEmpty()) {
                continue;
            }
            
            for (String command : commands) {
                String processedCommand = command.replace("{player}", sender.getName())
                                                .replace("{message}", originalMessage)
                                                .replace("{receiver}", player.getName());
                
                if (processedCommand.startsWith("[sound] ")) {
                    String soundData = processedCommand.substring(8).trim();
                    this.playSound(player, soundData);
                } else if (processedCommand.startsWith("[message] ")) {
                    String message = processedCommand.substring(10).trim();
                    message = HexUtils.translateAlternateColorCodes(message);
                    player.sendMessage(message);
                } else if (processedCommand.startsWith("[console-command] ")) {
                    String cmd = processedCommand.substring(18).trim();
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                    });
                } else if (processedCommand.startsWith("[player-command] ")) {
                    String cmd = processedCommand.substring(17).trim();
                    Bukkit.getScheduler().runTask(this.plugin, () -> {
                        player.performCommand(cmd);
                    });
                }
            }
            
            return;
        }
    }
    
    private void playSound(Player player, String soundData) {
        try {
            String[] parts = soundData.split(" ");
            String soundName = parts[0];
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1.0f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1.0f;
            
            Sound sound = Sound.valueOf(soundName);
            player.playSound(player.getLocation(), sound, volume, pitch);
        } catch (IllegalArgumentException e) {
        }
    }
}
