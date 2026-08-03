package fc.plugins.fcchat.integration;

import fc.plugins.fcchat.manager.config.ConfigManager;
import fc.plugins.fcchat.moderation.AiModerator;
import fc.plugins.fcchat.moderation.ToxicityStatistics;
import java.util.UUID;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class FcChatPlaceholderExpansion extends PlaceholderExpansion {
    private final AiModerator aiModerator;
    private final ConfigManager configManager;
    private final ToxicityStatistics toxicityStatistics;

    public FcChatPlaceholderExpansion(AiModerator aiModerator, ConfigManager configManager, ToxicityStatistics toxicityStatistics) {
        this.aiModerator = aiModerator;
        this.configManager = configManager;
        this.toxicityStatistics = toxicityStatistics;
    }

    public String getIdentifier() {
        return "fcchat";
    }

    public String getAuthor() {
        return "fcPlugins";
    }

    public String getVersion() {
        return "2.4";
    }

    public boolean persist() {
        return true;
    }

    public String onRequest(OfflinePlayer player, String params) {
        if (player == null) {
            return null;
        }
        if (params.equalsIgnoreCase("ai_toxicity")) {
            if (!this.aiModerator.isToxicityEnabled()) {
                return "N/A";
            }
            Integer toxicity = this.aiModerator.getToxicity(player.getUniqueId());
            if (toxicity == null) {
                return "N/A";
            }
            return toxicity + "%";
        }
        if (params.equalsIgnoreCase("ai_verdict")) {
            if (this.toxicityStatistics == null || !this.configManager.isAiModeratorStatisticsEnabled()) {
                return "N/A";
            }
            return this.toxicityStatistics.getVerdict(player.getUniqueId());
        }
        return null;
    }
}
