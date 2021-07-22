package co.mcsky.meweconomy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import co.mcsky.meweconomy.daily.DailyBalanceDataSource;
import co.mcsky.meweconomy.mituan.VipManager;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

@CommandAlias("meco|meweconomy")
public class MewEconomyCommands extends BaseCommand {

    private final PaperCommandManager commands;
    private final DailyBalanceDataSource dataSource;
    private final VipManager vipManager;

    public MewEconomyCommands(PaperCommandManager commands, DailyBalanceDataSource dataSource) {
        this.commands = commands;
        this.dataSource = dataSource;
        this.vipManager = new VipManager();
        registerCompletions();
        registerConditions();
    }

    private void registerCompletions() {
        commands.getCommandCompletions().registerCompletion("world", c -> Bukkit.getWorlds().stream().map(World::getName).toList());
    }

    private void registerConditions() {
        commands.getCommandConditions().addCondition(double.class, "limits", (c, exec, value) -> {
            if (value == null) {
                return;
            }
            if (c.hasConfig("min") && c.getConfigValue("min", 0) > value) {
                throw new ConditionFailedException("Min value must be " + c.getConfigValue("min", 0));
            }
            if (c.hasConfig("max") && c.getConfigValue("max", 100) < value) {
                throw new ConditionFailedException("Max value must be " + c.getConfigValue("max", 3));
            }
        });
    }

    /**
     * Warps the essentials /setawrp command
     */
    @Subcommand("setwarp")
    @CommandPermission("meweconomy.vip")
    @Syntax("<name>")
    public void setWarp(Player player, String name) {
        vipManager.setWarp(player, name);
    }

    @Subcommand("bal|balance")
    public void balance(CommandSender sender) {
        sender.sendMessage(MewEconomy.plugin.getMessage("command.system-balance.view",
                "balance", MewEconomy.plugin.getSystemAccount().getBalance()));
    }

    @Subcommand("day|daily")
    public void dailyBalance(Player player) {
        player.sendMessage(MewEconomy.plugin.getMessage(player, "command.daily-balance.view",
                "balance", dataSource.getPlayerModel(player.getUniqueId()).getDailyBalance()));
        player.sendMessage(MewEconomy.plugin.getMessage(player, "command.daily-balance.time",
                "time", dataSource.getPlayerModel(player.getUniqueId()).getCooldown().remainingTime(TimeUnit.HOURS)));
    }

    @Subcommand("take")
    @CommandPermission("meweconomy.admin")
    @CommandCompletion("@players @nothing")
    @Description("Take money from player and deposit it to system account")
    @Syntax("<player> <amount>")
    public void take(CommandSender sender, OfflinePlayer player, double amount) {
        double playerBalance = MewEconomy.plugin.getEco().getBalance(player);
        double withdraw = Math.min(playerBalance, amount);
        if (MewEconomy.plugin.getSystemAccount().withdrawToSystem(player, withdraw)) {
            sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.pay.success", "amount", withdraw));
        } else {
            sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.pay.failed"));
        }
    }

    @Subcommand("reload")
    @CommandPermission("meweconomy.admin")
    public void reload(CommandSender sender) {
        MewEconomy.plugin.loadLanguages();
        MewEconomy.plugin.config.load();
        sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.plugin-reloaded"));
    }

    @Subcommand("give")
    @CommandPermission("meweconomy.admin")
    @Description("Take money from the system account and deposit it to player")
    public class GiveCommand extends BaseCommand {

        @Subcommand("percent")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <percent(0-100)>")
        public void percent(CommandSender sender, OfflinePlayer player, @Conditions("limits:min=0,max=100") double percent) {
            SystemAccountUtils systemBalance = MewEconomy.plugin.getSystemAccount();
            double balance = systemBalance.getBalance();
            double withdraw = Math.min(balance, balance * percent / 100D);
            if (systemBalance.depositFromSystem(player, withdraw)) {
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.pay.success", "amount", withdraw));
            } else {
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.pay.failed"));
            }
        }

        @Subcommand("decimal")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <amount>")
        public void decimal(CommandSender sender, OfflinePlayer player, @Conditions("limits:min=0") double amount) {
            SystemAccountUtils systemBalance = MewEconomy.plugin.getSystemAccount();
            double balance = systemBalance.getBalance();
            double withdraw = Math.min(balance, amount);
            if (systemBalance.depositFromSystem(player, withdraw)) {
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.pay.success", "amount", withdraw));
            } else {
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.pay.failed"));
            }
        }


    }
}
