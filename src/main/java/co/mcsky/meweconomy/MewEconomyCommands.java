package co.mcsky.meweconomy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import co.mcsky.meweconomy.daily.DailyBalanceDatasource;
import co.mcsky.meweconomy.daily.DailyBalanceModel;
import co.mcsky.meweconomy.rice.RiceManager;
import co.mcsky.moecore.MoeCore;
import me.lucko.helper.Schedulers;
import me.lucko.helper.promise.Promise;
import me.lucko.helper.utils.Players;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@CommandAlias("meco|meweconomy")
public class MewEconomyCommands extends BaseCommand {

    private final PaperCommandManager commands;
    private final DailyBalanceDatasource dataSource;
    private final RiceManager riceManager;

    public MewEconomyCommands(PaperCommandManager commands, DailyBalanceDatasource dataSource, RiceManager riceManager) {
        this.commands = commands;
        this.dataSource = dataSource;
        this.riceManager = riceManager;
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

    @Subcommand("setwarp")
    @CommandPermission("meweconomy.vip")
    @Syntax("<name>")
    public void setWarp(Player player) {
        String warpName = player.getName().toLowerCase(); // force lowercase
        riceManager.setWarpCommand(player, warpName);
    }

    @Subcommand("bal|balance")
    public void balance(CommandSender sender) {
        sender.sendMessage(MewEconomy.plugin.getMessage("command.system-balance.view",
                "balance", MewEconomy.round(MoeCore.plugin.systemAccount().getBalance())));
    }

    private void sendMessageOnline(OfflinePlayer player, String message) {
        Players.get(player.getUniqueId()).ifPresent(p -> p.sendMessage(message));
    }

    @Subcommand("take")
    @CommandPermission("meweconomy.admin")
    @CommandCompletion("@players @nothing")
    @Description("Take money from player and deposit it to system account")
    @Syntax("<player> <amount>")
    public void take(CommandSender sender, OfflinePlayer player, double amount) {
        double playerBalance = MewEconomy.plugin.economy().getBalance(player);
        double withdraw = Math.min(playerBalance, amount);
        if (MoeCore.plugin.systemAccount().withdrawToSystem(player, withdraw)) {
            sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.take.sender-success", "amount", withdraw));
            sendMessageOnline(player, MewEconomy.plugin.getMessage("command.system-balance.take.receiver-success", "amount", withdraw));
        } else {
            sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.take.failed"));
        }
    }

    @Subcommand("save")
    @CommandPermission("meweconomy.admin")
    public void saveDatasource(CommandSender sender) {
        MewEconomy.plugin.saveDatasource();
        sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.datasource-saved"));
    }

    @Subcommand("reload")
    @CommandPermission("meweconomy.admin")
    public void reload(CommandSender sender) {
        MewEconomy.plugin.reload();
        sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.plugin-reloaded"));
    }

    @Subcommand("day|daily")
    public class DailyBalanceCommand extends BaseCommand {

        @Default
        public void balance(Player player) {
            player.sendMessage(MewEconomy.plugin.getMessage(player, "command.daily-balance.view",
                    "balance", MewEconomy.round(dataSource.getPlayerModel(player.getUniqueId()).getDailyBalance())));
            player.sendMessage(MewEconomy.plugin.getMessage(player, "command.daily-balance.time",
                    "time", dataSource.getPlayerModel(player.getUniqueId()).getCooldown().remainingTime(TimeUnit.HOURS)));
        }

        @Subcommand("add")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <amount>")
        public void add(CommandSender sender, String playerName, double amount) {
            final OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(playerName);
            if (player == null) {
                try {
                    Schedulers.async().run(() -> {
                        final OfflinePlayer fetchedPlayer = Bukkit.getOfflinePlayer(playerName);
                        sender.sendMessage("Fetching the UUID of name %s ...".formatted(playerName));
                        dataSource.getPlayerModel(fetchedPlayer).incrementDailyBalance(amount);
                        sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.daily-balance.add", "amount", amount, "player", fetchedPlayer.getName()));
                    }).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    sender.sendMessage(e.getLocalizedMessage());
                }
            } else {
                final DailyBalanceModel playerModel = dataSource.getPlayerModel(player);
                playerModel.incrementDailyBalance(amount);
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.daily-balance.add", "amount", amount, "player", player.getName()));
            }
        }

        @Subcommand("take")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <amount>")
        public void take(CommandSender sender, String playerName, double amount) {
            final OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(playerName);
            if (player == null) {
                try {
                    Promise.start().thenApplyAsync(e -> {
                        final OfflinePlayer fetchedPlayer = Bukkit.getOfflinePlayer(playerName);
                        sender.sendMessage("Fetching the UUID of name %s ...".formatted(playerName));
                        dataSource.getPlayerModel(fetchedPlayer).incrementDailyBalance(-amount);
                        return fetchedPlayer;
                    }).thenAcceptSync(p -> {
                        sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.daily-balance.take", "amount", amount, "player", p.getName()));
                    }).get(5, TimeUnit.SECONDS);
                } catch (InterruptedException | ExecutionException | TimeoutException e) {
                    sender.sendMessage(e.getLocalizedMessage());
                }
            } else {
                final DailyBalanceModel playerModel = dataSource.getPlayerModel(player);
                playerModel.incrementDailyBalance(-amount);
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.daily-balance.take", "amount", amount, "player", player.getName()));
            }
        }

    }

    @Subcommand("give")
    @CommandPermission("meweconomy.admin")
    @Description("Take money from the system account and deposit it to player")
    public class GiveCommand extends BaseCommand {

        private void depositFromSystem(CommandSender sender, OfflinePlayer player, double withdraw) {
            if (MoeCore.plugin.systemAccount().depositFromSystem(player, withdraw)) {
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.give.sender-success", "amount", MewEconomy.round(withdraw)));
                sendMessageOnline(player, MewEconomy.plugin.getMessage("command.system-balance.give.receiver-success", "amount", MewEconomy.round(withdraw)));
            } else {
                sender.sendMessage(MewEconomy.plugin.getMessage(sender, "command.system-balance.give.failed"));
            }
        }

        @Subcommand("percent")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <percent(0-100)>")
        public void percent(CommandSender sender, OfflinePlayer player, @Conditions("limits:min=0,max=100") double percent) {
            double balance = MoeCore.plugin.systemAccount().getBalance();
            double withdraw = Math.min(balance, balance * percent / 100D);
            depositFromSystem(sender, player, withdraw);
        }

        @Subcommand("decimal")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <amount>")
        public void decimal(CommandSender sender, OfflinePlayer player, @Conditions("limits:min=0") double amount) {
            double balance = MoeCore.plugin.systemAccount().getBalance();
            double withdraw = Math.min(balance, amount);
            depositFromSystem(sender, player, withdraw);
        }


    }
}
