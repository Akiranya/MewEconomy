package co.mcsky.meweconomy.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.CommandAlias;
import co.aikar.commands.annotation.CommandPermission;
import co.aikar.commands.annotation.Subcommand;
import co.aikar.commands.annotation.Syntax;
import co.mcsky.meweconomy.MewEconomy;
import org.bukkit.entity.Player;

@CommandAlias("%main")
@Subcommand("mituan")
public class MituanSimpleCommand extends BaseCommand {

    @Subcommand("setwarp")
    @CommandPermission("meco.vip")
    @Syntax("<name>")
    public void setWarp(Player player) {
        String warpName = player.getName().toLowerCase(); // force lowercase
        MewEconomy.plugin.getRiceManager().setWarpCommand(player, warpName);
    }

}
