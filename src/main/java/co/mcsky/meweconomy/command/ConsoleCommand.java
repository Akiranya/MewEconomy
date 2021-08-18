package co.mcsky.meweconomy.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.mcsky.meweconomy.MewEconomy;
import org.bukkit.command.CommandSender;

@CommandAlias("%main")
public class ConsoleCommand extends BaseCommand {

    @Subcommand("datasource save")
    @CommandPermission("meco.admin")
    public void saveDatasource(CommandSender sender) {
        MewEconomy.plugin.saveDatasource();
        sender.sendMessage(MewEconomy.plugin.message(sender, "command.datasource-saved"));
    }

    @Subcommand("datasource load")
    @CommandPermission("meco.admin")
    public void loadDatasource(CommandSender sender) {
        MewEconomy.plugin.loadDatasource();
        sender.sendMessage(MewEconomy.plugin.message(sender, "command.datasource-loaded"));
    }

    @Subcommand("reloadconfig")
    @CommandPermission("meco.admin")
    public void reload(CommandSender sender) {
        MewEconomy.plugin.reload();
        sender.sendMessage(MewEconomy.plugin.message(sender, "command.plugin-reloaded"));
    }

}
