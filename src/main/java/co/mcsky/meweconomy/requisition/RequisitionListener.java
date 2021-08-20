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
import net.kyori.adventure.text.Component;
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
        this.sellCooldownMap = CooldownMap.create(Cooldown.of(MewEconomy.plugin.config.sell_cooldown, TimeUnit.SECONDS));
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        Events.subscribe(RequisitionStartEvent.class).handler(this::onStart).bindWith(consumer);
        Events.subscribe(RequisitionSellEvent.class).handler(this::onSell).bindWith(consumer);
        Events.subscribe(RequisitionEndEvent.class).handler(this::onEnd).bindWith(consumer);
    }

    private void onSell(RequisitionSellEvent event) {
        if (!sellCooldownMap.test(event.getSeller().getUniqueId())) {
            RequisitionBus.sendMessage(event.getSeller(), MewEconomy.plugin.message(event.getSeller(), "command.requisition.seller.too-fast"));
            event.setCancelled(true);
            return;
        }

        RequisitionBus.broadcast(Component.text(MewEconomy.plugin.message(event.getSeller(), "command.requisition.sell"))
                .replaceText(builder -> builder.matchLiteral("{player}").replacement(event.getSeller().displayName()))
                .replaceText(builder -> builder.matchLiteral("{item}").replacement(event.getRequisition().getReqItem().displayName()))
                .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(event.getItemToSell().getAmount())))
                .replaceText(builder -> builder.matchLiteral("{remains}").replacement(Component.text(event.getRequisition().getRemains(event.getItemToSell().getAmount())).color(NamedTextColor.RED))));
    }

    private void onStart(RequisitionStartEvent event) {
        RequisitionBus.broadcast(Component.text(MewEconomy.plugin.message("command.requisition.req-init"))
                .replaceText(builder -> builder.matchLiteral("{player}").replacement(event.getRequisition().getBuyer().displayName()))
                .replaceText(builder -> builder.matchLiteral("{item}").replacement(event.getRequisition().getReqItem().displayName()))
                .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(event.getRequisition().getRemains())))
                .replaceText(builder -> builder.matchLiteral("{unit_price}").replacement(Component.text(event.getRequisition().getUnitPrice()).color(NamedTextColor.LIGHT_PURPLE)))
                .replaceText(builder -> builder.matchLiteral("{remaining_time}").replacement(Component.text(event.getRequisition().getDuration()))));
    }

    private void onEnd(RequisitionEndEvent event) {
        switch (event.getRequisitionEndReason()) {
            case AMOUNT_MET -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.done"));
            case TIMEOUT -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.timeout"));
            case CANCEL -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.cancel"));
            case ERROR -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.error"));
        }
    }
}
