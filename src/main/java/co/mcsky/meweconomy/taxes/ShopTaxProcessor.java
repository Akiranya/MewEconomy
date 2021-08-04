package co.mcsky.meweconomy.taxes;

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

import static co.mcsky.meweconomy.MewEconomy.plugin;

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
        // Listen the event earliest so that it can affect later events.
        Events.subscribe(CurrencyTransferEvent.class, EventPriority.LOWEST)
                .handler(e -> {
                    // we may not need so precise values, right?
                    double amountSent = e.getAmountSent().doubleValue();
                    double amountReceived = e.getAmountReceived().doubleValue();

                    double tax = 0D;
                    if (e.getPartner().equals(adminShopUUID)) {
                        // business logic 1: it's an admin shop

                        if (e.getDirection() == CurrencyTransferEvent.Direction.PARTNER) {
                            // player is buying items from admin shop

                            // make money and deposit to system account
                            tax = amountSent * (plugin.config.admin_shop_buy_tax_percent / 100D);
                            MoeCore.plugin.systemAccount().depositSystem(tax);

                            if (plugin.isDebugMode()) {
                                plugin.getLogger().info("System account received: %s".formatted(tax));
                            }
                        }
                    } else {
                        // business logic 2: it's a player shop

                        tax = amountReceived * (plugin.config.player_shop_tax_percent / 100D);
                        double amountReceivedTaxed = amountReceived - tax; // tax the receiver
                        e.setAmountReceived(BigDecimal.valueOf(amountReceivedTaxed));
                        MoeCore.plugin.systemAccount().depositSystem(tax);
                    }

                    if (plugin.isDebugMode()) {
                        plugin.getLogger().info("Admin shop UUID: %s".formatted(adminShopUUID));
                        plugin.getLogger().info("System account received: %s".formatted(tax));
                        plugin.getLogger().info("AmountSent: %s".formatted(e.getAmountSent()));
                        plugin.getLogger().info("AmountReceived: %s".formatted(e.getAmountReceived()));
                        plugin.getLogger().info("Initiator: %s".formatted(e.getInitiator().getUniqueId()));
                        plugin.getLogger().info("Partner: %s".formatted(e.getPartner()));
                        plugin.getLogger().info("Sender: %s".formatted(e.getSender()));
                        plugin.getLogger().info("Receiver: %s".formatted(e.getReceiver()));
                        plugin.getLogger().info("Direction: %s".formatted(e.getDirection()));
                    }
                }).bindWith(consumer);
    }
}
