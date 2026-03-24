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
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

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
                } catch (ClassNotFoundException var8) {
                    try {
                        Class.forName("com.mysql.jdbc.Driver");
                    } catch (ClassNotFoundException var7) {
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
            } catch (SQLException var9) {
                this.enabled = false;
            } catch (Exception var10) {
                this.enabled = false;
            }

        }, this.executor);
    }

    private void createTables() {
        if (this.enabled && this.connection != null) {
            try {
                String tablePrefix = this.mysqlConfig.getString("mysql.table-prefix");
                String createTable = "CREATE TABLE IF NOT EXISTS " + tablePrefix + "messages (id INT AUTO_INCREMENT PRIMARY KEY,server_name VARCHAR(50) NOT NULL,player_name VARCHAR(50) NOT NULL,player_uuid VARCHAR(36) NOT NULL,message TEXT NOT NULL,channel_type VARCHAR(20) NOT NULL,timestamp BIGINT NOT NULL,INDEX idx_timestamp (timestamp),INDEX idx_server (server_name))";

                try {
                    PreparedStatement stmt = this.connection.prepareStatement(createTable);

                    try {
                        stmt.executeUpdate();
                    } catch (Throwable var7) {
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (Throwable var6) {
                                var7.addSuppressed(var6);
                            }
                        }

                        throw var7;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException var8) {
                    this.enabled = false;
                }
            } catch (Exception var9) {
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

                try {
                    PreparedStatement stmt = this.connection.prepareStatement(query);

                    try {
                        stmt.setString(1, serverName);
                        stmt.setString(2, playerName);
                        stmt.setString(3, playerUuid);
                        stmt.setString(4, message);
                        stmt.setString(5, channelType);
                        stmt.setLong(6, System.currentTimeMillis());
                        stmt.executeUpdate();
                    } catch (Throwable var12) {
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (Throwable var11) {
                                var12.addSuppressed(var11);
                            }
                        }

                        throw var12;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException var13) {
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
            } catch (SQLException var2) {
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

    public void getNewMessages(long lastMessageId, Consumer<List<MySQLManager.SyncMessage>> callback) {
        if (this.enabled && this.isSyncEnabled()) {
            CompletableFuture.runAsync(() -> {
                String tablePrefix = this.mysqlConfig.getString("mysql.table-prefix");
                String currentServer = this.mysqlConfig.getString("sync.server-name");
                String query = "SELECT id, server_name, player_name, player_uuid, message, channel_type, timestamp FROM " + tablePrefix + "messages WHERE id > ? AND server_name != ? ORDER BY id ASC LIMIT 50";

                try {
                    PreparedStatement stmt = this.connection.prepareStatement(query);

                    try {
                        stmt.setLong(1, lastMessageId);
                        stmt.setString(2, currentServer);
                        ResultSet rs = stmt.executeQuery();

                        try {
                            ArrayList messages = new ArrayList();

                            while(rs.next()) {
                                MySQLManager.SyncMessage message = new MySQLManager.SyncMessage(rs.getLong("id"), rs.getString("server_name"), rs.getString("player_name"), rs.getString("player_uuid"), rs.getString("message"), rs.getString("channel_type"), rs.getLong("timestamp"));
                                messages.add(message);
                            }

                            if (!messages.isEmpty()) {
                                Bukkit.getScheduler().runTask(this.plugin, () -> {
                                    callback.accept(messages);
                                });
                            }
                        } catch (Throwable var13) {
                            if (rs != null) {
                                try {
                                    rs.close();
                                } catch (Throwable var12) {
                                    var13.addSuppressed(var12);
                                }
                            }

                            throw var13;
                        }

                        if (rs != null) {
                            rs.close();
                        }
                    } catch (Throwable var14) {
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (Throwable var11) {
                                var14.addSuppressed(var11);
                            }
                        }

                        throw var14;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }
                } catch (SQLException var15) {
                }

            }, this.executor);
        }
    }

    public long getLastMessageId() {
        if (this.enabled && this.connection != null) {
            try {
                String tablePrefix = this.mysqlConfig.getString("mysql.table-prefix");
                String query = "SELECT MAX(id) as max_id FROM " + tablePrefix + "messages";
                PreparedStatement stmt = this.connection.prepareStatement(query);

                label81: {
                    long var7;
                    try {
                        ResultSet rs = stmt.executeQuery();

                        label83: {
                            try {
                                if (rs.next()) {
                                    long maxId = rs.getLong("max_id");
                                    var7 = maxId;
                                    break label83;
                                }
                            } catch (Throwable var11) {
                                if (rs != null) {
                                    try {
                                        rs.close();
                                    } catch (Throwable var10) {
                                        var11.addSuppressed(var10);
                                    }
                                }

                                throw var11;
                            }

                            if (rs != null) {
                                rs.close();
                            }
                            break label81;
                        }

                        if (rs != null) {
                            rs.close();
                        }
                    } catch (Throwable var12) {
                        if (stmt != null) {
                            try {
                                stmt.close();
                            } catch (Throwable var9) {
                                var12.addSuppressed(var9);
                            }
                        }

                        throw var12;
                    }

                    if (stmt != null) {
                        stmt.close();
                    }

                    return var7;
                }

                if (stmt != null) {
                    stmt.close();
                }
            } catch (SQLException var13) {
            }

            return 0L;
        } else {
            return 0L;
        }
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