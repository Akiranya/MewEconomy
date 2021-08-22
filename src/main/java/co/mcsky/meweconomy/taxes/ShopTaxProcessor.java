package co.mcsky.meweconomy.taxes;

import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.moecore.MoeCore;
import com.Acrobot.ChestShop.Configuration.Properties;
import com.Acrobot.ChestShop.Events.Economy.CurrencyTransferEvent;
import me.lucko.helper.Events;
import me.lucko.helper.profiles.OfflineModeProfiles;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import org.bukkit.event.EventPriority;
import org.jetbrains.annotations.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Modifies the amount of money in the ChestShop transaction.
 */
public class ShopTaxProcessor implements TerminableModule {

    private final UUID adminShopUUID;

    public ShopTaxProcessor() {
        this.adminShopUUID = OfflineModeProfiles.getUniqueId(Properties.ADMIN_SHOP_NAME);
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        // listen to the event the earliest
        // so that it can affect later events

        Events.subscribe(CurrencyTransferEvent.class, EventPriority.LOWEST).handler(e -> {
            double amountSent = e.getAmountSent().doubleValue();
            double amountReceived = e.getAmountReceived().doubleValue();

            double tax = 0D;
            if (e.getPartner().equals(adminShopUUID)) {
                // case 1: it's an admin shop

                if (e.getDirection() == CurrencyTransferEvent.Direction.PARTNER) {
                    // player is buying items from admin shop

                    // make money and deposit to system account
                    tax = amountSent * (MewEconomy.config().admin_shop_buy_tax_percent / 100D);
                    MoeCore.systemAccount().depositSystem(tax);

                    if (MewEconomy.config().debug) {
                        MewEconomy.logger().info("System account received: %s".formatted(tax));
                    }
                }
            } else {
                // case 2: it's a player shop

                tax = amountReceived * (MewEconomy.config().player_shop_tax_percent / 100D);
                double amountReceivedTaxed = amountReceived - tax; // tax the receiver
                e.setAmountReceived(BigDecimal.valueOf(amountReceivedTaxed));
                MoeCore.systemAccount().depositSystem(tax);
            }

            if (MewEconomy.config().debug) {
                MewEconomy.logger().info("Admin shop UUID: %s".formatted(adminShopUUID));
                MewEconomy.logger().info("System account received: %s".formatted(tax));
                MewEconomy.logger().info("AmountSent: %s".formatted(e.getAmountSent()));
                MewEconomy.logger().info("AmountReceived: %s".formatted(e.getAmountReceived()));
                MewEconomy.logger().info("Initiator: %s".formatted(e.getInitiator().getUniqueId()));
                MewEconomy.logger().info("Partner: %s".formatted(e.getPartner()));
                MewEconomy.logger().info("Sender: %s".formatted(e.getSender()));
                MewEconomy.logger().info("Receiver: %s".formatted(e.getReceiver()));
                MewEconomy.logger().info("Direction: %s".formatted(e.getDirection()));
            }
        }).bindWith(consumer);
    }
}
