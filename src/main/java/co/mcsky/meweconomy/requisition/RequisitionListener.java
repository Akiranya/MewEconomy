package co.mcsky.meweconomy.requisition;

import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.meweconomy.requisition.event.RequisitionEndEvent;
import co.mcsky.meweconomy.requisition.event.RequisitionSellEvent;
import co.mcsky.meweconomy.requisition.event.RequisitionStartEvent;
import me.lucko.helper.Events;
import me.lucko.helper.cooldown.Cooldown;
import me.lucko.helper.cooldown.CooldownMap;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import net.kyori.adventure.text.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Adds extra control & information on the requisition.
 */
public class RequisitionListener implements TerminableModule {

    private final CooldownMap<UUID> sellCooldownMap;

    public RequisitionListener() {
        this.sellCooldownMap = CooldownMap.create(Cooldown.of(MewEconomy.config().sell_cooldown, TimeUnit.SECONDS));
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        Events.subscribe(RequisitionStartEvent.class).handler(this::onStart).bindWith(consumer);
        Events.subscribe(RequisitionSellEvent.class).handler(this::onSell).bindWith(consumer);
        Events.subscribe(RequisitionEndEvent.class).handler(this::onEnd).bindWith(consumer);
    }

    private void onSell(RequisitionSellEvent event) {
        if (!sellCooldownMap.test(event.getSeller().getUniqueId())) {
            RequisitionBus.sendMessage(event.getSeller(), MewEconomy.text("command.requisition.seller.too-fast"));
            event.setCancelled(true);
            return;
        }

        RequisitionBus.broadcast(MewEconomy.text3("command.requisition.sell")
                .replace("player", event.getSeller())
                .replace("item", event.getItemToSell())
                .replace("amount", event.getItemToSell().getAmount())
                .replace("remains", event.getRequisition().getRemains(event.getItemToSell().getAmount()), s -> s.color(NamedTextColor.RED))
                .asComponent());
    }

    private void onStart(RequisitionStartEvent event) {
        RequisitionBus.broadcast(MewEconomy.text3("command.requisition.req-init")
                .replace("player", event.getRequisition().getBuyer())
                .replace("item", event.getRequisition().getReqItem())
                .replace("amount", event.getRequisition().getRemains())
                .replace("unit_price", event.getRequisition().getUnitPrice(), s -> s.color(NamedTextColor.LIGHT_PURPLE))
                .replace("remaining_time", event.getRequisition().getDuration()).asComponent());
    }

    private void onEnd(RequisitionEndEvent event) {
        switch (event.getRequisitionEndReason()) {
            case AMOUNT_MET -> RequisitionBus.broadcast(MewEconomy.text("command.requisition.end.done"));
            case TIMEOUT -> RequisitionBus.broadcast(MewEconomy.text("command.requisition.end.timeout"));
            case CANCEL -> RequisitionBus.broadcast(MewEconomy.text("command.requisition.end.cancel"));
            case ERROR -> RequisitionBus.broadcast(MewEconomy.text("command.requisition.end.error"));
        }
    }
}
