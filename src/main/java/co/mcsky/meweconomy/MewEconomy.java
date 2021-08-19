package co.mcsky.meweconomy;

import co.aikar.commands.PaperCommandManager;
import co.mcsky.meweconomy.daily.DailyBalanceDatasource;
import co.mcsky.meweconomy.daily.DailyBalanceFileHandler;
import co.mcsky.meweconomy.daily.DailyBalanceProcessor;
import co.mcsky.meweconomy.requisition.RequisitionBus;
import co.mcsky.meweconomy.rice.RiceManager;
import co.mcsky.meweconomy.taxes.ShopTaxProcessor;
import de.themoep.utils.lang.bukkit.LanguageManager;
import me.lucko.helper.Schedulers;
import me.lucko.helper.Services;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class MewEconomy extends ExtendedJavaPlugin {

    public static MewEconomy plugin;
    public MewEconomyConfig config;

    private LanguageManager lang;
    private Economy eco;

    private DailyBalanceFileHandler dailyBalanceFileHandler;
    private DailyBalanceDatasource dailyBalanceDatasource;
    private RiceManager riceManager;

    public static double round(double value) {
        double scale = Math.pow(10, Math.max(1, MewEconomy.plugin.config.decimal_round + 1));
        return Math.round(value * scale) / scale;
    }

    @Override
    protected void enable() {
        plugin = this;

        // load vault services
        try {
            eco = Services.load(Economy.class);
        } catch (IllegalStateException e) {
            getLogger().severe(e.getMessage());
            getLogger().severe("Some vault registration is not present");
            disable();
            return;
        }

        config = new MewEconomyConfig();
        config.load();
        config.save();

        // load data source from file
        dailyBalanceFileHandler = new DailyBalanceFileHandler(getDataFolder());
        dailyBalanceDatasource = dailyBalanceFileHandler.load().orElseGet(DailyBalanceDatasource::new);

        // schedule task to save data periodically
        Schedulers.async().runRepeating(() -> {
            dailyBalanceFileHandler.save(getDailyDatasource());
            getLogger().info("Datasource saved successfully!");
        }, 0, TimeUnit.SECONDS, config.save_interval, TimeUnit.SECONDS).bindWith(this);

        // register modules
        bindModule(new ShopTaxProcessor());
        bindModule(new DailyBalanceProcessor());
        bind(RequisitionBus.INSTANCE);
        riceManager = bindModule(new RiceManager());

        loadLanguages();
        registerCommands();
    }

    @Override
    protected void disable() {
        if (dailyBalanceFileHandler != null) {
            dailyBalanceFileHandler.save(dailyBalanceDatasource);
        }
    }

    public boolean debugMode() {
        return config.debug;
    }

    public void registerCommands() {
        PaperCommandManager commands = new PaperCommandManager(this);

        // replacements have to be added here
        commands.getCommandReplacements().addReplacement("%main", "meco");
        commands.getCommandReplacements().addReplacement("%req", "req");

        // then initialize the command entry (each command is initialized inside)
        new MewEconomyCommands(commands);
    }

    public void loadLanguages() {
        this.lang = new LanguageManager(this, "languages", "zh");
        this.lang.setPlaceholderPrefix("{");
        this.lang.setPlaceholderSuffix("}");
        this.lang.setProvider(sender -> {
            if (sender instanceof Player) {
                return ((Player) sender).locale().getLanguage();
            }
            return null;
        });
    }

    public void loadDatasource() {
        dailyBalanceDatasource = dailyBalanceFileHandler.load().orElseGet(DailyBalanceDatasource::new);
    }

    public void saveDatasource() {
        dailyBalanceFileHandler.save(dailyBalanceDatasource);
    }

    public void reload() {
        MewEconomy.plugin.loadLanguages();
        MewEconomy.plugin.config.load();
    }

    public Economy economy() {
        return eco;
    }

    public RiceManager getRiceManager() {
        return riceManager;
    }

    public DailyBalanceDatasource getDailyDatasource() {
        return dailyBalanceDatasource;
    }

    public String message(CommandSender sender, String key, Object... replacements) {
        if (replacements.length == 0) {
            return lang.getConfig(sender).get(key);
        } else {
            String[] list = new String[replacements.length];
            for (int i = 0; i < replacements.length; i++) {
                if (replacements[i] instanceof Double n) {
                    list[i] = Double.toString(round(n));
                } else {
                    list[i] = replacements[i].toString();
                }
            }
            return lang.getConfig(sender).get(key, list);
        }
    }

    public String message(String key, Object... replacements) {
        return message(plugin.getServer().getConsoleSender(), key, replacements);
    }
}
