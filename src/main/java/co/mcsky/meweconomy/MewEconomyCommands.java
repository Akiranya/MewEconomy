package co.mcsky.meweconomy;

import co.aikar.commands.ConditionFailedException;
import co.aikar.commands.PaperCommandManager;
import co.mcsky.meweconomy.command.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;

import java.util.Arrays;

public record MewEconomyCommands(PaperCommandManager commands) {

    public MewEconomyCommands(PaperCommandManager commands) {
        this.commands = commands;

        // must first register conditions & completions
        registerConditions();
        registerCompletions();

        // then register each command
        commands.registerCommand(new ConsoleCommand());
        commands.registerCommand(new DailyBalanceCommand());
        commands.registerCommand(new MituanSimpleCommand());
        commands.registerCommand(new SystemBalanceCommand());
        commands.registerCommand(new RequisitionCommand());
    }

    private void registerCompletions() {
        commands.getCommandCompletions().registerCompletion("world", c -> Bukkit.getWorlds().stream().map(World::getName).toList());
        commands.getCommandCompletions().registerAsyncCompletion("materials", c -> Arrays.stream(Material.values()).map(mat -> mat.name().toLowerCase()).toList());
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

}
