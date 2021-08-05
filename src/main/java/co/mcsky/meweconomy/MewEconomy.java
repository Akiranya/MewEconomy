package co.mcsky.meweconomy;

import co.aikar.commands.PaperCommandManager;
import co.mcsky.meweconomy.daily.DailyBalanceDatasource;
import co.mcsky.meweconomy.daily.DailyBalanceFileHandler;
import co.mcsky.meweconomy.daily.DailyBalanceProcessor;
import co.mcsky.meweconomy.limit.OpenHoursProcessor;
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
    public LanguageManager lang;

    private Economy eco;

    private DailyBalanceFileHandler dailyBalanceFileHandler;
    private DailyBalanceDatasource dailyBalanceDatasource;
    private RiceManager riceManager;

    public static double round(double value) {
        double scale = Math.pow(10, MewEconomy.plugin.config.decimal_round);
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
            dailyBalanceFileHandler.save(dailyBalanceDatasource);
            getLogger().info("Data source saved successfully!");
        }, 0, TimeUnit.SECONDS, config.save_interval, TimeUnit.SECONDS).bindWith(this);

        // register modules
        bindModule(new ShopTaxProcessor());
        bindModule(new OpenHoursProcessor());
        bindModule(new DailyBalanceProcessor(dailyBalanceDatasource));
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
        commands.registerCommand(new MewEconomyCommands(commands, dailyBalanceDatasource, riceManager));
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
        // TODO async loads
        dailyBalanceDatasource = dailyBalanceFileHandler.load().orElseGet(DailyBalanceDatasource::new);
    }

    public void saveDatasource() {
        // TODO async saves
        dailyBalanceFileHandler.save(dailyBalanceDatasource);
    }

    public void reload() {
        MewEconomy.plugin.loadLanguages();
        MewEconomy.plugin.config.load();
    }

    public Economy economy() {
        return eco;
    }

    public String message(CommandSender sender, String key, Object... replacements) {
        if (replacements.length == 0) {
            return lang.getConfig(sender).get(key);
        } else {
            String[] list = new String[replacements.length];
            for (int i = 0; i < replacements.length; i++) {
                list[i] = replacements[i].toString();
            }
            return lang.getConfig(sender).get(key, list);
        }
    }

    public String message(String key, Object... replacements) {
        return message(plugin.getServer().getConsoleSender(), key, replacements);
    }

    public DailyBalanceDatasource getDailyBalanceDatasource() {
        return dailyBalanceDatasource;
    }
}
