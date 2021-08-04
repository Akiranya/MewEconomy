package co.mcsky.meweconomy.daily;

import org.bukkit.OfflinePlayer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a saved database.
 */
public class DailyBalanceDatasource {

    private final Map<UUID, DailyBalanceModel> playerModelMap;

    public DailyBalanceDatasource() {
        this.playerModelMap = new HashMap<>();
    }

    public void addPlayerModel(DailyBalanceModel playerModel) {
        this.playerModelMap.put(playerModel.getPlayerUUID(), playerModel);
    }

    public void addPlayerModels(List<DailyBalanceModel> playerModels) {
        for (DailyBalanceModel p : playerModels) {
            this.playerModelMap.put(p.getPlayerUUID(), p);
        }
    }

    public DailyBalanceModel getPlayerModel(OfflinePlayer offlinePlayer) {
        return getPlayerModel(offlinePlayer.getUniqueId());
    }

    public DailyBalanceModel getPlayerModel(UUID playerUUID) {
        if (playerModelMap.containsKey(playerUUID)) {
            return playerModelMap.get(playerUUID);
        }
        DailyBalanceModel added = new DailyBalanceModel(playerUUID);
        playerModelMap.put(playerUUID, added);
        return added;
    }

    public Map<UUID, DailyBalanceModel> getPlayerModelMap() {
        return playerModelMap;
    }

    public List<DailyBalanceModel> getPlayerModels() {
        return playerModelMap.values().stream().toList();
    }

    public void removePlayerModel(UUID playerUUID) {
        playerModelMap.remove(playerUUID);
    }

    public boolean hasPlayerModel(UUID playerUUID) {
        return playerModelMap.containsKey(playerUUID);
    }
}
