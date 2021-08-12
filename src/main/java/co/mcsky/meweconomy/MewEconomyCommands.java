package co.mcsky.meweconomy;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import co.aikar.commands.annotation.*;
import co.mcsky.meweconomy.daily.DailyBalanceModel;
import co.mcsky.moecore.MoeCore;
import me.lucko.helper.Schedulers;
import me.lucko.helper.utils.Players;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@SuppressWarnings("unused")
@CommandAlias("meco|meweconomy")
public class MewEconomyCommands extends BaseCommand {

    private final PaperCommandManager commands;

    public MewEconomyCommands(PaperCommandManager commands) {
        this.commands = commands;
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
        MewEconomy.plugin.getRiceManager().setWarpCommand(player, warpName);
    }

    @Subcommand("bal|balance")
    public void balance(CommandSender sender) {
        sender.sendMessage(MewEconomy.plugin.message("command.system-balance.view",
                "balance", MewEconomy.round(MoeCore.plugin.systemAccount().getBalance())));
    }

    private void sendMessageOnline(OfflinePlayer player, String message) {
        Players.get(player.getUniqueId()).ifPresent(p -> p.sendMessage(message));
    }

    @Subcommand("datasource save")
    @CommandPermission("meweconomy.admin")
    public void saveDatasource(CommandSender sender) {
        MewEconomy.plugin.saveDatasource();
        sender.sendMessage(MewEconomy.plugin.message(sender, "command.datasource-saved"));
    }

    @Subcommand("datasource load")
    @CommandPermission("meweconomy.admin")
    public void loadDatasource(CommandSender sender) {
        MewEconomy.plugin.loadDatasource();
        sender.sendMessage(MewEconomy.plugin.message(sender, "command.datasource-loaded"));
    }

    @Subcommand("reloadconfig")
    @CommandPermission("meweconomy.admin")
    public void reload(CommandSender sender) {
        MewEconomy.plugin.reload();
        sender.sendMessage(MewEconomy.plugin.message(sender, "command.plugin-reloaded"));
    }

    @Subcommand("take")
    @CommandPermission("meweconomy.admin")
    @CommandCompletion("@players @nothing")
    @Syntax("<player> <amount>")
    public void take(CommandSender sender, OfflinePlayer player, double amount) {
        double playerBalance = MewEconomy.plugin.economy().getBalance(player);
        double withdraw = Math.min(playerBalance, amount);
        if (MoeCore.plugin.systemAccount().withdrawToSystem(player, withdraw)) {
            sender.sendMessage(MewEconomy.plugin.message(sender, "command.system-balance.take.sender-success", "amount", withdraw));
            sendMessageOnline(player, MewEconomy.plugin.message("command.system-balance.take.receiver-success", "amount", withdraw));
        } else {
            sender.sendMessage(MewEconomy.plugin.message(sender, "command.system-balance.take.failed"));
        }
    }

    @Subcommand("day|daily")
    public class DailyBalanceCommand extends BaseCommand {

        @Default
        public void balance(Player player) {
            final DailyBalanceModel model = MewEconomy.plugin.getDailyDatasource().getPlayerModel(player.getUniqueId());
            model.testResetBalance();
            player.sendMessage(MewEconomy.plugin.message(player, "command.daily-balance.view", "balance", MewEconomy.round(model.getDailyBalance())));
            player.sendMessage(MewEconomy.plugin.message(player, "command.daily-balance.time", "time", model.getCooldown().remainingTime(TimeUnit.HOURS)));
        }

        @Subcommand("view")
        @Syntax("<player>")
        @CommandPermission("meweconomy.admin")
        public void view(CommandSender sender, OfflinePlayer player) {
            sender.sendMessage(MewEconomy.plugin.message(sender, "command.daily-balance.view-others",
                    "player", player.getName(), "balance", MewEconomy.round(MewEconomy.plugin.getDailyDatasource().getPlayerModel(player.getUniqueId()).getDailyBalance())));
        }

        @Subcommand("add")
        @CommandPermission("meweconomy.admin")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <amount>")
        public void add(CommandSender sender, String playerName, double amount) {
            updateByName(sender, playerName, amount, p -> sender.sendMessage(MewEconomy.plugin.message(sender, "command.daily-balance.add", "amount", amount, "player", p.getName())));
        }

        @Subcommand("take")
        @CommandPermission("meweconomy.admin")
        @CommandCompletion("@players @nothing")
        @Syntax("<player> <amount>")
        public void take(CommandSender sender, String playerName, double amount) {
            updateByName(sender, playerName, -amount, p -> sender.sendMessage(MewEconomy.plugin.message(sender, "command.daily-balance.take", "amount", amount, "player", p.getName())));
        }

        private void updateByName(CommandSender sender, String playerName, double amount, Consumer<OfflinePlayer> promptCallback) {
            // its just a convenient method to not repeat code in add() and take() below
            final OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(playerName);
            if (player == null) {
                Schedulers.async().run(() -> {
                    MewEconomy.plugin.getLogger().info("Fetching the UUID of name %s ...".formatted(playerName));
                    final OfflinePlayer fetchedPlayer = Bukkit.getOfflinePlayer(playerName);
                    if (!fetchedPlayer.hasPlayedBefore()) {
                        sender.sendMessage(MewEconomy.plugin.message(sender, "command.daily-balance.player-dose-not-exist"));
                        return;
                    }
                    MewEconomy.plugin.getDailyDatasource().getPlayerModel(fetchedPlayer).incrementBalance(amount);
                    promptCallback.accept(fetchedPlayer);
                });
            } else {
                final DailyBalanceModel playerModel = MewEconomy.plugin.getDailyDatasource().getPlayerModel(player);
                playerModel.incrementBalance(amount);
                promptCallback.accept(player);
            }
        }

    }

    @Subcommand("give")
    @CommandPermission("meweconomy.admin")
    public class GiveCommand extends BaseCommand {

        private void depositFromSystem(CommandSender sender, OfflinePlayer player, double withdraw) {
            if (MoeCore.plugin.systemAccount().depositFromSystem(player, withdraw)) {
                sender.sendMessage(MewEconomy.plugin.message(sender, "command.system-balance.give.sender-success", "amount", MewEconomy.round(withdraw)));
                sendMessageOnline(player, MewEconomy.plugin.message("command.system-balance.give.receiver-success", "amount", MewEconomy.round(withdraw)));
            } else {
                sender.sendMessage(MewEconomy.plugin.message(sender, "command.system-balance.give.failed"));
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
