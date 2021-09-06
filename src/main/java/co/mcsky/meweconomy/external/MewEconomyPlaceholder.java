package co.mcsky.meweconomy.external;

import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.moecore.MoeCore;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class MewEconomyPlaceholder extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return "meco";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Nailm";
    }

    @Override
    public @NotNull String getVersion() {
        return "0.1";
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        params = params.toLowerCase(Locale.ROOT);
        switch (params) {
            case "daily_balance" -> MewEconomy.dailyDatasource().getPlayerModel(player).getDailyBalanceString();
            case "system_balance" -> MoeCore.systemAccount().getSystemBalanceString(MewEconomy.config().decimal_round);
        }
        return null;
    }

}
