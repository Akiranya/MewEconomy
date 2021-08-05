package co.mcsky.meweconomy.daily;

import co.mcsky.meweconomy.MewEconomy;
import me.lucko.helper.cooldown.Cooldown;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Represents a player in the daily balance system.
 */
public class DailyBalanceModel {

    private final UUID playerUUID;
    private double dailyBalance;
    private Cooldown cooldown; // in mills

    public DailyBalanceModel(UUID playerUUID) {
        this.playerUUID = playerUUID;
        this.dailyBalance = MewEconomy.plugin.config.daily_balance; // 1000 RuanMeiBi daily
        this.cooldown = getDefaultCooldown();
    }

    public DailyBalanceModel(UUID playerUUID, double dailyBalance, Cooldown cooldown) {
        this.playerUUID = playerUUID;
        this.dailyBalance = dailyBalance;
        this.cooldown = cooldown;
    }

    public static Cooldown getDefaultCooldown() {
        return Cooldown.of(MewEconomy.plugin.config.daily_balance_timeout, TimeUnit.MILLISECONDS);
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public double getDailyBalance() {
        return dailyBalance;
    }

    public void setBalance(double newBalance) {
        dailyBalance = Math.min(MewEconomy.plugin.config.daily_balance, newBalance);
    }

    public boolean isBalanceFull() {
        return dailyBalance >= MewEconomy.plugin.config.daily_balance;
    }

    public void incrementBalance(double amount) {
        dailyBalance += amount;
        dailyBalance = Math.min(MewEconomy.plugin.config.daily_balance, Math.max(0D, dailyBalance)); // integrity check
    }

    public void resetBalance() {
        dailyBalance = MewEconomy.plugin.config.daily_balance;
    }

    public Cooldown getCooldown() {
        return cooldown;
    }

    public void setCooldown(Cooldown cooldown) {
        this.cooldown = cooldown;
    }

}
