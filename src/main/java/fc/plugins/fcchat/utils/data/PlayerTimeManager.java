package fc.plugins.fcchat.utils.data;
import org.bukkit.Statistic;
import org.bukkit.entity.Player;

public class PlayerTimeManager {
    private final Statistic playtimeStatistic;

    public PlayerTimeManager() {
        this.playtimeStatistic = this.resolvePlaytimeStatistic();
    }

    public long getTimeSinceFirstJoin(Player player) {
        return this.getWorldPlaytimeMillis(player);
    }

    private Statistic resolvePlaytimeStatistic() {
        try {
            return Statistic.valueOf("PLAY_ONE_MINUTE");
        } catch (IllegalArgumentException ignored) {
            try {
                return Statistic.valueOf("PLAY_ONE_TICK");
            } catch (IllegalArgumentException ignoredToo) {
                return Statistic.PLAY_ONE_MINUTE;
            }
        }
    }

    private long getWorldPlaytimeMillis(Player player) {
        try {
            int ticks = player.getStatistic(this.playtimeStatistic);
            return ticks * 50L;
        } catch (Exception ignored) {
            return 0L;
        }
    }
}
