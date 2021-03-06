package co.mcsky.meweconomy.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.meweconomy.daily.DailyBalanceModel;
import me.lucko.helper.Schedulers;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@CommandAlias("%main")
@Subcommand("day")
public class DailyBalanceCommand extends BaseCommand {

    @Default
    public void balance(Player player) {
        final DailyBalanceModel model = MewEconomy.dailyDatasource().getPlayerModel(player.getUniqueId());
        model.testResetBalance();
        player.sendMessage(MewEconomy.text("command.daily-balance.view", "balance", model.getDailyBalanceString()));
        player.sendMessage(MewEconomy.text("command.daily-balance.time", "time", model.getCooldown().remainingTime(TimeUnit.HOURS)));
    }

    @Subcommand("view")
    @CommandPermission("meco.admin")
    @CommandCompletion("@players")
    @Syntax("<player>")
    public void view(CommandSender sender, OfflinePlayer player) {
        final DailyBalanceModel model = MewEconomy.dailyDatasource().getPlayerModel(player.getUniqueId());
        sender.sendMessage(MewEconomy.text("command.daily-balance.view-others", "player", player.getName(), "balance", model.getDailyBalanceString()));
    }

    @Subcommand("add")
    @CommandPermission("meco.admin")
    @CommandCompletion("@players @nothing")
    @Syntax("<player> <amount>")
    public void add(CommandSender sender, String playerName, double amount) {
        updateByName(sender, playerName, amount, p -> sender.sendMessage(MewEconomy.text("command.daily-balance.add", "amount", amount, "player", p.getName())));
    }

    @Subcommand("take")
    @CommandPermission("meco.admin")
    @CommandCompletion("@players @nothing")
    @Syntax("<player> <amount>")
    public void take(CommandSender sender, String playerName, double amount) {
        updateByName(sender, playerName, -amount, p -> sender.sendMessage(MewEconomy.text("command.daily-balance.take", "amount", amount, "player", p.getName())));
    }

    private void updateByName(CommandSender sender, String playerName, double amount, Consumer<OfflinePlayer> promptCallback) {
        // it's just a convenient method to not repeat code in add() and take() below
        final OfflinePlayer player = Bukkit.getOfflinePlayerIfCached(playerName);
        if (player == null) {
            Schedulers.async().run(() -> {
                MewEconomy.logger().info("Fetching the UUID of name %s ...".formatted(playerName));
                final OfflinePlayer fetchedPlayer = Bukkit.getOfflinePlayer(playerName);
                if (!fetchedPlayer.hasPlayedBefore()) {
                    sender.sendMessage(MewEconomy.text("command.daily-balance.player-dose-not-exist"));
                    return;
                }
                MewEconomy.dailyDatasource().getPlayerModel(fetchedPlayer).incrementBalance(amount);
                promptCallback.accept(fetchedPlayer);
            });
        } else {
            final DailyBalanceModel playerModel = MewEconomy.dailyDatasource().getPlayerModel(player);
            playerModel.incrementBalance(amount);
            promptCallback.accept(player);
        }
    }

}
