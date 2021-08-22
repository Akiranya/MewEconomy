package co.mcsky.meweconomy.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.moecore.MoeCore;
import me.lucko.helper.utils.Players;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;

@CommandAlias("%main")
@Subcommand("sys")
public class SystemBalanceCommand extends BaseCommand {

    @Default
    @Subcommand("balance")
    public void balance(CommandSender sender) {
        sender.sendMessage(MewEconomy.text("command.system-balance.view", "balance", MoeCore.systemAccount().getBalance()));
    }

    @Subcommand("take")
    @CommandPermission("meco.admin")
    @CommandCompletion("@players @nothing")
    @Syntax("<player> <amount>")
    public void take(CommandSender sender, OfflinePlayer player, double amount) {
        double playerBalance = MewEconomy.economy().getBalance(player);
        double withdraw = Math.min(playerBalance, amount);
        if (MoeCore.systemAccount().withdrawToSystem(player, withdraw)) {
            sender.sendMessage(MewEconomy.text("command.system-balance.take.sender-success", "amount", withdraw));
            sendMessageOnline(player, MewEconomy.text("command.system-balance.take.receiver-success", "amount", withdraw));
        } else {
            sender.sendMessage(MewEconomy.text("command.system-balance.take.failed"));
        }
    }

    @Subcommand("give percent")
    @CommandPermission("meco.admin")
    @CommandCompletion("@players @nothing")
    @Syntax("<player> <percent(0-100)>")
    public void percent(CommandSender sender, OfflinePlayer player, @Conditions("limits:min=0,max=100") double percent) {
        double balance = MoeCore.systemAccount().getBalance();
        double withdraw = Math.min(balance, balance * percent / 100D);
        depositFromSystem(sender, player, withdraw);
    }

    @Subcommand("give decimal")
    @CommandPermission("meco.admin")
    @CommandCompletion("@players @nothing")
    @Syntax("<player> <amount>")
    public void decimal(CommandSender sender, OfflinePlayer player, @Conditions("limits:min=0") double amount) {
        double balance = MoeCore.systemAccount().getBalance();
        double withdraw = Math.min(balance, amount);
        depositFromSystem(sender, player, withdraw);
    }

    private void sendMessageOnline(OfflinePlayer player, String message) {
        Players.get(player.getUniqueId()).ifPresent(p -> p.sendMessage(message));
    }

    private void depositFromSystem(CommandSender sender, OfflinePlayer player, double withdraw) {
        if (MoeCore.systemAccount().depositFromSystem(player, withdraw)) {
            sender.sendMessage(MewEconomy.text("command.system-balance.give.sender-success", "amount", withdraw));
            sendMessageOnline(player, MewEconomy.text("command.system-balance.give.receiver-success", "amount", withdraw));
        } else {
            sender.sendMessage(MewEconomy.text("command.system-balance.give.failed"));
        }
    }
}
