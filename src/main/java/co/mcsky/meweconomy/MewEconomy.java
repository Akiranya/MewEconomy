package co.mcsky.meweconomy;

import co.aikar.commands.PaperCommandManager;
import co.mcsky.meweconomy.daily.DailyBalanceDatasource;
import co.mcsky.meweconomy.daily.DailyBalanceFileHandler;
import co.mcsky.meweconomy.daily.DailyBalanceProcessor;
import co.mcsky.meweconomy.requisition.RequisitionBus;
import co.mcsky.meweconomy.rice.MituanHub;
import co.mcsky.meweconomy.taxes.ShopTaxProcessor;
import co.mcsky.moecore.text.Text;
import co.mcsky.moecore.text.TextRepository;
import de.themoep.utils.lang.bukkit.LanguageManager;
import me.lucko.helper.Schedulers;
import me.lucko.helper.Services;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MewEconomy extends ExtendedJavaPlugin {

    public static MewEconomy plugin;

    private MewEconomyConfig config;
    private LanguageManager languageManager;
    private TextRepository textRepository;
    private Economy eco;
    private MituanHub mituan;
    private DailyBalanceFileHandler dailyBalanceFileHandler;
    private DailyBalanceDatasource dailyBalanceDatasource;

    public static Logger logger() {
        return plugin.getLogger();
    }

    public static MewEconomyConfig config() {
        return plugin.config;
    }

    public static String text(String key, Object... replacements) {
        if (replacements.length == 0) {
            return plugin.languageManager.getDefaultConfig().get(key);
        } else {
            String[] list = new String[replacements.length];
            for (int i = 0; i < replacements.length; i++) {
                if (replacements[i] instanceof Double || replacements[i] instanceof Float) {
                    list[i] = BigDecimal.valueOf(((Number) replacements[i]).doubleValue()).setScale(config().decimal_round, RoundingMode.HALF_UP).toPlainString();
                } else {
                    list[i] = replacements[i].toString();
                }
            }
            return plugin.languageManager.getDefaultConfig().get(key, list);
        }
    }

    public static Text text3(String key) {
        return plugin.textRepository.get(key);
    }

    public static Economy economy() {
        return plugin.eco;
    }

    public static MituanHub mituan() {
        return plugin.mituan;
    }

    public static DailyBalanceDatasource dailyDatasource() {
        return plugin.dailyBalanceDatasource;
    }

    public void loadDatasource() {
        dailyBalanceDatasource = dailyBalanceFileHandler.load().orElseGet(DailyBalanceDatasource::new);
    }

    public void saveDatasource() {
        dailyBalanceFileHandler.save(dailyBalanceDatasource);
    }

    public void reload() {
        MewEconomy.plugin.loadLanguages();
        MewEconomy.config().load();
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
            dailyBalanceFileHandler.save(dailyDatasource());
            getLogger().info("Datasource saved successfully!");
        }, 0, TimeUnit.SECONDS, config.save_interval, TimeUnit.SECONDS).bindWith(this);

        // register modules
        bindModule(new ShopTaxProcessor());
        bindModule(new DailyBalanceProcessor());
        bindModule(RequisitionBus.INSTANCE);
        mituan = bindModule(new MituanHub());

        loadLanguages();
        registerCommands();
    }

    @Override
    protected void disable() {
        if (dailyBalanceFileHandler != null) {
            dailyBalanceFileHandler.save(dailyBalanceDatasource);
        }
    }

    private void registerCommands() {
        PaperCommandManager commands = new PaperCommandManager(this);

        // replacements have to be added here
        commands.getCommandReplacements().addReplacement("%main", "meco");
        commands.getCommandReplacements().addReplacement("%req", "req");

        // then initialize the command entry (each command is initialized inside)
        new MewEconomyCommands(commands);
    }

    private void loadLanguages() {
        this.languageManager = new LanguageManager(this, "languages", "zh");
        this.languageManager.setPlaceholderPrefix("{");
        this.languageManager.setPlaceholderSuffix("}");
        this.languageManager.setProvider(sender -> {
            if (sender instanceof Player) {
                return ((Player) sender).locale().getLanguage();
            }
            return null;
        });
        this.textRepository = new TextRepository(MewEconomy::text);
    }

}
