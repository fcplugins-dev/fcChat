
package fc.plugins.fcchat.integration.database;

import fc.plugins.fcchat.FcChat;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;

public class MySQLManager {
    private final FcChat plugin;
    private FileConfiguration mysqlConfig;
    private Connection connection;
    private ExecutorService executor;
    private boolean enabled;

    public MySQLManager(FcChat plugin) {
        this.plugin = plugin;
        this.executor = Executors.newFixedThreadPool(3);
        this.loadConfig();
        if (this.enabled) {
            this.connect();
        }
    }

    private void loadConfig() {
        File mysqlFile = new File(this.plugin.getDataFolder(), "mysql.yml");
        if (!mysqlFile.exists()) {
            this.plugin.saveResource("mysql.yml", false);
        }
        this.mysqlConfig = YamlConfiguration.loadConfiguration(mysqlFile);
        this.enabled = this.mysqlConfig.getBoolean("mysql.enabled");
    }

    private void connect() {
        CompletableFuture.runAsync(() -> {
            try {
                if (this.connection != null && !this.connection.isClosed()) {
                    return;
                }
                try {
                    Class.forName("com.mysql.cj.jdbc.Driver");
                }
                catch (ClassNotFoundException var8) {
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                    }
                    catch (ClassNotFoundException var7) {
                        this.enabled = false;
                        return;
                    }
                }
                String host = this.mysqlConfig.getString("mysql.host");
                int port = this.mysqlConfig.getInt("mysql.port");
                String database = this.mysqlConfig.getString("mysql.database");
                String username = this.mysqlConfig.getString("mysql.username");
                String password = this.mysqlConfig.getString("mysql.password");
                String url = "jdbc:mysql://" + host + ":" + port + "/" + database + "?autoReconnect=true&useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=utf8&connectTimeout=60000&socketTimeout=60000&serverTimezone=UTC&useLegacyDatetimeCode=false";
                this.connection = DriverManager.getConnection(url, username, password);
                this.createTables();
            }
            catch (SQLException var9) {
                this.enabled = false;
            }
            catch (Exception var10) {
                this.enabled = false;
            }
        }, this.executor);
    }

    private void createTables() {
        if (this.enabled && this.connection != null) {
            try {
                String tablePrefix = this.mysqlConfig.getString("mysql.table-prefix");
                String createTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "messages (id INT AUTO_INCREMENT PRIMARY KEY,server_name VARCHAR(50) NOT NULL,player_name VARCHAR(50) NOT NULL,player_uuid VARCHAR(36) NOT NULL,message TEXT NOT NULL,channel_type VARCHAR(20) NOT NULL,timestamp BIGINT NOT NULL,INDEX idx_timestamp (timestamp),INDEX idx_server (server_name))";
                try (PreparedStatement stmt = this.connection.prepareStatement(createTable);){
                    stmt.executeUpdate();
                }
                catch (SQLException var8) {
                    this.enabled = false;
                }
            }
            catch (Exception var9) {
                this.enabled = false;
            }
        }
    }

    public void saveMessage(String playerName, String playerUuid, String message, String channelType) {
        if (this.enabled && this.isSyncEnabled()) {
            CompletableFuture.runAsync(() -> {
                String tablePrefix = this.mysqlConfig.getString("mysql.table-prefix");
                String serverName = this.mysqlConfig.getString("sync.server-name");
                String query = "INSERT INTO " + tablePrefix + "messages (server_name, player_name, player_uuid, message, channel_type, timestamp) VALUES (?, ?, ?, ?, ?, ?)";
                try (PreparedStatement stmt = this.connection.prepareStatement(query);){
                    stmt.setString(1, serverName);
                    stmt.setString(2, playerName);
                    stmt.setString(3, playerUuid);
                    stmt.setString(4, message);
                    stmt.setString(5, channelType);
                    stmt.setLong(6, System.currentTimeMillis());
                    stmt.executeUpdate();
                }
                catch (SQLException sQLException) {
                    // empty catch block
                }
            }, this.executor);
        }
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public boolean isSyncEnabled() {
        return this.enabled && this.mysqlConfig.getBoolean("sync.enabled");
    }

    public boolean shouldSyncChannel(String channelType) {
        return this.mysqlConfig.getStringList("sync.sync-channels").contains(channelType);
    }

    public void disconnect() {
        if (this.connection != null) {
            try {
                this.connection.close();
            }
            catch (SQLException sQLException) {
                // empty catch block
            }
        }
        if (this.executor != null) {
            this.executor.shutdown();
        }
    }

    public void reconnect() {
        this.disconnect();
        if (this.enabled) {
            this.connect();
        }
    }

    public void getNewMessages(long lastMessageId, Consumer<List<SyncMessage>> callback) {
        if (this.enabled && this.isSyncEnabled()) {
            CompletableFuture.runAsync(() -> {
                String tablePrefix = this.mysqlConfig.getString("mysql.table-prefix");
                String currentServer = this.mysqlConfig.getString("sync.server-name");
                String query = "SELECT id, server_name, player_name, player_uuid, message, channel_type, timestamp FROM " + tablePrefix + "messages WHERE id > ? AND server_name != ? ORDER BY id ASC LIMIT 50";
                try (PreparedStatement stmt = this.connection.prepareStatement(query);){
                    stmt.setLong(1, lastMessageId);
                    stmt.setString(2, currentServer);
                    try (ResultSet rs = stmt.executeQuery();){
                        ArrayList<SyncMessage> messages = new ArrayList<SyncMessage>();
                        while (rs.next()) {
                            SyncMessage message = new SyncMessage(rs.getLong("id"), rs.getString("server_name"), rs.getString("player_name"), rs.getString("player_uuid"), rs.getString("message"), rs.getString("channel_type"), rs.getLong("timestamp"));
                            messages.add(message);
                        }
                        if (!messages.isEmpty()) {
                            this.plugin.getCompatScheduler().runGlobal(() -> callback.accept(messages));
                        }
                    }
                }
                catch (SQLException sQLException) {
                    // empty catch block
                }
            }, this.executor);
        }
    }

    public long getLastMessageId() {
        if (this.enabled && this.connection != null) {
            block15: {
                try {
                    long var7;
                    String tablePrefix = this.mysqlConfig.getString("mysql.table-prefix");
                    String query = "SELECT MAX(id) as max_id FROM " + tablePrefix + "messages";
                    try (PreparedStatement stmt = this.connection.prepareStatement(query);
                         ResultSet rs = stmt.executeQuery();){
                        long maxId;
                        if (!rs.next()) break block15;
                        var7 = maxId = rs.getLong("max_id");
                    }
                    return var7;
                }
                catch (SQLException sQLException) {
                    // empty catch block
                }
            }
            return 0L;
        }
        return 0L;
    }

    public static class SyncMessage {
        public final long id;
        public final String serverName;
        public final String playerName;
        public final String playerUuid;
        public final String message;
        public final String channelType;
        public final long timestamp;

        public SyncMessage(long id, String serverName, String playerName, String playerUuid, String message, String channelType, long timestamp) {
            this.id = id;
            this.serverName = serverName;
            this.playerName = playerName;
            this.playerUuid = playerUuid;
            this.message = message;
            this.channelType = channelType;
            this.timestamp = timestamp;
        }
    }
}
