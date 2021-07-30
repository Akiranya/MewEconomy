package co.mcsky.meweconomy;

import cat.nyaa.nyaacore.component.ISystemBalance;
import cat.nyaa.nyaacore.component.NyaaComponent;
import co.aikar.commands.PaperCommandManager;
import co.mcsky.meweconomy.daily.DailyBalanceProcessor;
import co.mcsky.meweconomy.daily.DailyBalanceDataSource;
import co.mcsky.meweconomy.daily.DailyBalanceFileHandler;
import co.mcsky.meweconomy.limit.OpenHoursProcessor;
import co.mcsky.meweconomy.rice.RiceManager;
import co.mcsky.meweconomy.taxes.ShopTaxProcessor;
import de.themoep.utils.lang.bukkit.LanguageManager;
import me.lucko.helper.Schedulers;
import me.lucko.helper.Services;
import me.lucko.helper.plugin.ExtendedJavaPlugin;
import net.luckperms.api.LuckPerms;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.concurrent.TimeUnit;

public class MewEconomy extends ExtendedJavaPlugin {

    public static MewEconomy plugin;

    public MewEconomyConfig config;
    public LanguageManager lang;

    private Economy eco;
    private LuckPerms lp;
    private SystemAccountUtils systemBalance;

    private DailyBalanceFileHandler dailyBalanceFileHandler;
    private DailyBalanceDataSource dailyBalanceDataSource;
    private RiceManager riceManager;

    @Override
    protected void enable() {
        plugin = this;

        // load vault services
        try {
            this.eco = Services.load(Economy.class);
            this.lp = Services.load(LuckPerms.class);
        } catch (IllegalStateException e) {
            getLogger().severe(e.getMessage());
            getLogger().severe("Some vault registration is not present");
            disable();
            return;
        }

        // after vault is loaded successfully, initialize system account
        this.systemBalance = new SystemAccountUtils();

        // register NyaaCore ISystemBalance component
        // so that all fee functions of NyaaUtils
        // can link to the Towny server account
        NyaaComponent.register(ISystemBalance.class, this.systemBalance);

        this.config = new MewEconomyConfig();
        this.config.load();
        this.config.save();

        // load data source from file
        dailyBalanceFileHandler = new DailyBalanceFileHandler();
        dailyBalanceDataSource = dailyBalanceFileHandler.load().orElseGet(() -> {
            getLogger().warning("Data file does not exist, creating new instance");
            return new DailyBalanceDataSource();
        });

        // schedule task to save data periodically
        Schedulers.async().runRepeating(() -> {
            dailyBalanceFileHandler.save(dailyBalanceDataSource);
            getLogger().info("Data source saved successfully!");
        }, 0, TimeUnit.SECONDS, this.config.save_interval, TimeUnit.SECONDS).bindWith(this);

        // register modules
        bindModule(new ShopTaxProcessor(systemBalance));
        bindModule(new OpenHoursProcessor());
        bindModule(new DailyBalanceProcessor(dailyBalanceDataSource));
        riceManager = bindModule(new RiceManager());

        loadLanguages();
        registerCommands();
    }

    @Override
    protected void disable() {
        // save data source into file
        if (dailyBalanceFileHandler != null)
            dailyBalanceFileHandler.save(dailyBalanceDataSource);
    }

    public boolean isDebugMode() {
        return config.debug;
    }

    public void registerCommands() {
        PaperCommandManager commands = new PaperCommandManager(this);
        commands.registerCommand(new MewEconomyCommands(commands, dailyBalanceDataSource, riceManager));
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

    public void reload() {
        // TODO can reload data source
    }

    public Economy getEco() {
        return eco;
    }

    public LuckPerms getPerm() {
        return lp;
    }

    public SystemAccountUtils getSystemAccount() {
        return systemBalance;
    }

    public String getMessage(CommandSender sender, String key, Object... replacements) {
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

    public String getMessage(String key, Object... replacements) {
        return getMessage(plugin.getServer().getConsoleSender(), key, replacements);
    }

    public DailyBalanceDataSource getDailyBalanceDataSource() {
        return dailyBalanceDataSource;
    }
}
