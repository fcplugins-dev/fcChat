package fc.plugins.fcchat.channel;

import org.bukkit.configuration.ConfigurationSection;

public class Channel {
    private String id;
    private String name;
    private String format;
    private String permission;
    private boolean enabled;
    private int radius;

    public Channel(String id, String name, String format, String permission, boolean enabled) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.permission = permission;
        this.enabled = enabled;
    }

    public static Channel fromConfig(ConfigurationSection section, String id) {
        String name = section.getString("name", id);
        String format = section.getString("format", "&7[&b" + name + "&7] &f{player}: &7{message}");
        String permission = section.getString("permission", "fcchat.channel." + id);
        boolean enabled = section.getBoolean("enabled", true);

        return new Channel(id, name, format, permission, enabled);
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

    public boolean isEnabled() {
        return enabled;
    }

} 