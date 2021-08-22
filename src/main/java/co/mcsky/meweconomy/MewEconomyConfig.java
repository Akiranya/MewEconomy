package co.mcsky.meweconomy;

import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MewEconomyConfig {
    public static final String FILENAME = "config.yml";

    private final YamlConfigurationLoader loader;

    /* config nodes start */

    public boolean debug;
    public long save_interval;
    public int decimal_round;
    public double daily_balance;
    public double daily_balance_buy_percent;
    public long daily_balance_timeout;
    public boolean daily_balance_remind_full;
    public double admin_shop_buy_tax_percent;
    public double player_shop_tax_percent;
    public boolean vip_enabled;
    public String vip_group_name;
    public String vip_shared_warp_group_name;
    public long vip_set_warp_cooldown;
    public int requisition_duration;
    public List<Integer> broadcast_times;
    public int sell_cooldown;
    public boolean allow_sell_to_self;

    /* config nodes end */

    private CommentedConfigurationNode root;

    public MewEconomyConfig() {
        loader = YamlConfigurationLoader.builder()
                .file(new File(MewEconomy.plugin.getDataFolder(), FILENAME))
                .indent(2)
                .build();
    }

    public void load() {
        try {
            root = loader.load();
        } catch (ConfigurateException e) {
            MewEconomy.logger().severe(e.getMessage());
            MewEconomy.plugin.getServer().getPluginManager().disablePlugin(MewEconomy.plugin);
        }

        /* initialize config nodes */

        try {
            debug = root.node("debug").getBoolean(true);
            decimal_round = root.node("decimal_round").getInt(3);
            save_interval = root.node("save-interval").getLong(300L);

            daily_balance = root.node("daily-balance").getDouble(1000D);
            daily_balance_buy_percent = root.node("daily-balance-buy-percent").getDouble(25D);
            daily_balance_timeout = root.node("daily-balance-timeout").getLong(TimeUnit.HOURS.toMillis(20));
            daily_balance_remind_full = root.node("daily-balance-remind-full").getBoolean(false);

            admin_shop_buy_tax_percent = root.node("admin-shop-buy-tax-percent").getDouble(100D);
            player_shop_tax_percent = root.node("player-shop-tax-percent").getDouble(25D);

            vip_enabled = root.node("vip-enabled").getBoolean(false);
            vip_group_name = root.node("vip-group-name").getString("vip");
            vip_shared_warp_group_name = root.node("vip-shared-warp-group-name").getString("vip_shared_warps");
            vip_set_warp_cooldown = root.node("vip-setwarp-cooldown").getLong(TimeUnit.HOURS.toMillis(20));

            requisition_duration = root.node("requisition-duration").getInt(120);
            broadcast_times = root.node("broadcast-times").getList(Integer.class, List.of(90, 60, 30, 10, 5));
            sell_cooldown = root.node("sell-cooldown").getInt(3);
            allow_sell_to_self = root.node("allow-sell-to-self").getBoolean(false);
        } catch (SerializationException e) {
            e.printStackTrace();
        }
    }

    public void save() {
        try {
            loader.save(root);
        } catch (ConfigurateException e) {
            MewEconomy.logger().severe(e.getMessage());
        }
    }

}
