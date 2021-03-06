package co.mcsky.meweconomy.daily;

import co.mcsky.meweconomy.MewEconomy;
import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Events.PreShopCreationEvent;
import com.Acrobot.ChestShop.Events.PreTransactionEvent;
import com.Acrobot.ChestShop.Signs.ChestShopSign;
import me.lucko.helper.Events;
import me.lucko.helper.cooldown.Cooldown;
import me.lucko.helper.cooldown.CooldownMap;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class DailyBalanceProcessor implements TerminableModule {

    private final CooldownMap<Player> messageReminderCooldown;

    public DailyBalanceProcessor() {
        this.messageReminderCooldown = CooldownMap.create(Cooldown.of(3, TimeUnit.SECONDS));
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        Events.subscribe(PlayerJoinEvent.class)
                .filter(e -> !MewEconomy.dailyDatasource().hasPlayerModel(e.getPlayer().getUniqueId()))
                .handler(this::onJoin)
                .bindWith(consumer);
        Events.subscribe(PreTransactionEvent.class)
                .filter(e -> ChestShopSign.isAdminShop(e.getSign())) // only handle admin shops
                .handler(this::onTransaction)
                .bindWith(consumer);
        Events.subscribe(PreShopCreationEvent.class)
                .handler(this::onShopCreate)
                .bindWith(consumer);
    }

    private void onJoin(PlayerJoinEvent e) {
        // add data to data source when new player joins

        MewEconomy.dailyDatasource().addPlayerModel(new DailyBalanceModel(e.getPlayer().getUniqueId()));
        if (MewEconomy.config().debug) {
            MewEconomy.logger().info("Adding player model to data source: %s".formatted(e.getPlayer().getName()));
        }
    }

    private void onTransaction(PreTransactionEvent e) {
        // listen to admin transactions to control daily balance

        final Player player = e.getClient();
        final DailyBalanceModel model = MewEconomy.dailyDatasource().getPlayerModel(player.getUniqueId());
        model.testResetBalance();

        // the price on the admin shop
        final double price = e.getExactPrice().doubleValue();

        switch (e.getTransactionType()) {
            case BUY -> { // the player is buying items from admin shops
                double increment = price * MewEconomy.config().daily_balance_buy_percent / 100D;
                model.incrementBalance(increment);
                if (MewEconomy.config().daily_balance_remind_full && model.isBalanceFull()) {
                    // adds cooldown to not send messages too often
                    if (messageReminderCooldown.test(player)) {
                        player.sendMessage(MewEconomy.text("chat.reach-daily-balance"));
                    }
                }
            }
            case SELL -> { // the player is selling items to admin shops
                if (model.getDailyBalance() < price) {
                    e.setCancelled(PreTransactionEvent.TransactionOutcome.OTHER);
                    player.sendMessage(MewEconomy.text("chat.insufficient-daily-balance", "required_amount", price, "daily_balance", model.getDailyBalance()));
                    return;
                }
                model.incrementBalance(-price);
            }
        }
    }

    private void onShopCreate(PreShopCreationEvent e) {
        // add checks when creating admin shop, unifying all admin shop owner name.

        if (ChestShopSign.isAdminShop(e.getSign())) {
            String line = e.getSign().line(0).toString();
            String adminShopName = Properties.ADMIN_SHOP_NAME;
            if (!line.equals(adminShopName)) {
                e.setSignLine((byte) 0, adminShopName);
            }
        }
    }


}
