package fc.plugins.fcchat.chat.channel;

import org.bukkit.configuration.ConfigurationSection;

public class Channel {
    private String id;
    private String name;
    private String format;
    private String permission;
    private String symbol;
    private boolean enabled;

    public Channel(String id, String name, String format, String permission, String symbol, boolean enabled) {
        this.id = id;
        this.name = name;
        this.format = format;
        this.permission = permission;
        this.symbol = symbol;
        this.enabled = enabled;
    }

    public static Channel fromConfig(ConfigurationSection section, String id) {
        String name = section.getString("name", id);
        String format = section.getString("format", "&7[&b" + name + "&7] {player}: &7{message}");
        String permission = section.getString("permission", "fcchat.channel." + id);
        String symbol = section.getString("symbol", null);
        boolean enabled = section.getBoolean("enabled", true);
        return new Channel(id, name, format, permission, symbol, enabled);
    }

    public String getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public String getFormat() {
        return this.format;
    }

    public String getPermission() {
        return this.permission;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isClanChannel() {
        return this.id.equalsIgnoreCase("clan");
    }

    public String getSymbol() {
        return this.symbol;
    }

    public boolean hasSymbol() {
        return this.symbol != null && !this.symbol.isEmpty();
    }
}