package fc.plugins.fcchat.channel;

import org.bukkit.configuration.ConfigurationSection;

public class Channel {
    private String id;
    private String name;
    private String format;
    private String permission;
    private String placeholder;
    private String placeholderNoClan;
    private boolean enabled;
    private int radius;

    public Channel(String id, String name, String format, String permission, String placeholder, String placeholderNoClan, boolean enabled) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.permission = permission;
        this.placeholder = placeholder;
        this.placeholderNoClan = placeholderNoClan;
        this.enabled = enabled;
    }

    public static Channel fromConfig(ConfigurationSection section, String id) {
        String name = section.getString("name", id);
        String format = section.getString("format", "&7[&b" + name + "&7] &f{player}: &7{message}");
        String permission = section.getString("permission", "fcchat.channel." + id);
        String placeholder = section.getString("placeholder", null);
        String placeholderNoClan = section.getString("placeholder_no_clan", "&7Нету");
        boolean enabled = section.getBoolean("enabled", true);

        return new Channel(id, name, format, permission, placeholder, placeholderNoClan, enabled);
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getFormat() {
        return format;
    }

    public String getPermission() {
        return permission;
    }

    public String getPlaceholder() {
        return placeholder;
    }

    public String getPlaceholderNoClan() {
        return placeholderNoClan;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isClanChannel() {
        return placeholder != null;
    }
} 