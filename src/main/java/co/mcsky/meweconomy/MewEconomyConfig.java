package co.mcsky.meweconomy;

import co.mcsky.meweconomy.config.YamlConfigFactory;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.concurrent.TimeUnit;

public class MewEconomyConfig {
    public static final String FILENAME = "config.yml";

    /* config nodes start */

    public boolean debug;
    public long save_interval;

    public double daily_balance;
    public double daily_balance_buy_percent;
    public long daily_balance_timeout;
    public boolean daily_balance_remind_full;

    public double admin_shop_buy_tax_percent;
    public double player_shop_tax_percent;

    public boolean vip_enabled;
    public String vip_group_name;
    public String vip_shared_warp_group_name;
    public long vip_setwarp_cooldown;

    /* config nodes end */

    private YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public MewEconomyConfig() {
        loader = YamlConfigFactory.loader(new File(MewEconomy.plugin.getDataFolder(), FILENAME));
    }

    public void load() {
        try {
            root = loader.load();
        } catch (ConfigurateException e) {
            MewEconomy.plugin.getLogger().severe(e.getMessage());
            MewEconomy.plugin.getServer().getPluginManager().disablePlugin(MewEconomy.plugin);
        }

        /* initialize config nodes */

        debug = root.node("debug").getBoolean(true);
        save_interval = root.node("save-interval").getLong(300L);
        daily_balance = root.node("daily-balance").getDouble(1000D);
        daily_balance_buy_percent = root.node("daily-balance-buy-percent").getDouble(80D);
        daily_balance_timeout = root.node("daily-balance-timeout").getLong(TimeUnit.HOURS.toMillis(20));
        daily_balance_remind_full = root.node("daily-balance-remind-full").getBoolean(false);
        admin_shop_buy_tax_percent = root.node("admin-shop-buy-tax-percent").getDouble(100D);
        player_shop_tax_percent = root.node("player-shop-tax-percent").getDouble(10D);
        vip_enabled = root.node("vip-enabled").getBoolean(false);
        vip_group_name = root.node("vip-group-name").getString("vip");
        vip_shared_warp_group_name = root.node("vip-shared-warp-group-name").getString("vip_shared_warps");
        vip_setwarp_cooldown = root.node("vip-setwarp-cooldown").getLong(TimeUnit.HOURS.toMillis(20));
    }

    public void save() {
        try {
            loader.save(root);
        } catch (ConfigurateException e) {
            MewEconomy.plugin.getLogger().severe(e.getMessage());
        }
    }

}
