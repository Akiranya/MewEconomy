package co.mcsky.meweconomy.daily;

import co.mcsky.meweconomy.MewEconomy;
import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import me.lucko.helper.Events;
import me.lucko.helper.cooldown.Cooldown;
import me.lucko.helper.metadata.Metadata;
import me.lucko.helper.metadata.MetadataKey;
import me.lucko.helper.metadata.MetadataMap;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class ChestShopDailyBalanceProcessor implements TerminableModule {

    private final DailyBalanceDataSource dataSource;

    public ChestShopDailyBalanceProcessor(DailyBalanceDataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        /* Add data to data source when new player joins */
        Events.subscribe(PlayerJoinEvent.class)
                .filter(e -> !dataSource.hasPlayerModel(e.getPlayer().getUniqueId()))
                .handler(e -> {
                    dataSource.addPlayerModel(new DailyBalanceModel(e.getPlayer().getUniqueId()));
                    if (MewEconomy.plugin.isDebugMode()) {
                        MewEconomy.plugin.getLogger().info("Adding player model to data source: %s".formatted(e.getPlayer().getName()));
                    }
                }).bindWith(consumer);

        /* add checks when creating admin shop, unifying all admin shop owner name. */
        Events.subscribe(PreShopCreationEvent.class)
                .handler(e -> {
                    if (ChestShopSign.isAdminShop(e.getSign())) {
                        String line = e.getSign().line(0).toString();
                        String adminShopName = Properties.ADMIN_SHOP_NAME;
                        if (!line.equals(adminShopName)) {
                            e.setSignLine((byte) 0, adminShopName);
                        }
                    }
                }).bindWith(consumer);

        /* listen to admin transactions to control daily balance */

        // add cooldown for the reminder of full daily balance
        MetadataKey<Cooldown> REACH_DAILY_BAL_MSG = MetadataKey.createCooldownKey("reach-daily-balance");
        Events.subscribe(PlayerJoinEvent.class).handler(e -> Metadata.provideForPlayer(e.getPlayer()).put(REACH_DAILY_BAL_MSG, Cooldown.of(3, TimeUnit.SECONDS)));

        Events.subscribe(PreTransactionEvent.class)
                .filter(e -> ChestShopSign.isAdminShop(e.getSign())) // only handle admin shops
                .handler(e -> {
                    final Player player = e.getClient();
                    final DailyBalanceModel model = dataSource.getPlayerModel(player.getUniqueId());
                    if (model.getCooldown().test()) {
                        model.resetDailyBalance();
                        if (MewEconomy.plugin.isDebugMode()) {
                            MewEconomy.plugin.getLogger().info("Player %s's daily balance reset");
                        }
                    }

                    final double price = e.getExactPrice().doubleValue();
                    switch (e.getTransactionType()) {
                        case BUY -> {
                            double increment = price * MewEconomy.plugin.config.daily_balance_buy_percent / 100D;
                            model.incrementDailyBalance(increment); // increment daily balance

                            if (MewEconomy.plugin.config.daily_balance_remind_full && model.isDailyBalanceFull()) {
                                final MetadataMap metadataMap = Metadata.provideForPlayer(player);
                                final Optional<Cooldown> cooldown = metadataMap.get(REACH_DAILY_BAL_MSG);
                                if (cooldown.isPresent() && cooldown.get().test()) {
                                    // don't send messages too often
                                    player.sendMessage(MewEconomy.plugin.getMessage(player, "chat.reach-daily-balance"));
                                }
                            }
                        }
                        case SELL -> {
                            if (model.getDailyBalance() - price < 0) {
                                e.setCancelled(PreTransactionEvent.TransactionOutcome.OTHER);
                                player.sendMessage(MewEconomy.plugin.getMessage(player, "chat.insufficient-daily-balance",
                                        "required_amount", price, "daily_balance", model.getDailyBalance()));
                                return;
                            }
                            model.incrementDailyBalance(-price); // decrement daily balance
                        }
                    }
                }).bindWith(consumer);
    }
}
