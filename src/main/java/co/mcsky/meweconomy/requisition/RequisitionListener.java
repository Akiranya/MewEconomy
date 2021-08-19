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

    private final CooldownMap<UUID> cooldownMap;

    public RequisitionListener() {
        this.cooldownMap = CooldownMap.create(Cooldown.of(MewEconomy.plugin.config.sell_cooldown, TimeUnit.SECONDS));
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        Events.subscribe(RequisitionStartEvent.class).handler(e -> {
            // listen to start event

            RequisitionBus.broadcast(Component.text(MewEconomy.plugin.message("command.requisition.req-init"))
                    .replaceText(builder -> builder.matchLiteral("{player}").replacement(e.getRequisition().getBuyer().displayName()))
                    .replaceText(builder -> builder.matchLiteral("{item}").replacement(e.getRequisition().getReqItem().displayName()))
                    .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(e.getRequisition().getRemains())))
                    .replaceText(builder -> builder.matchLiteral("{unit_price}").replacement(Component.text(e.getRequisition().getUnitPrice()).color(NamedTextColor.LIGHT_PURPLE)))
                    .replaceText(builder -> builder.matchLiteral("{remaining_time}").replacement(Component.text(e.getRequisition().getDuration()))));
        }).bindWith(consumer);

        Events.subscribe(RequisitionEndEvent.class).handler(e -> {
            // listen to end event

            switch (e.getRequisitionEndReason()) {
                case AMOUNT_MET -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.done"));
                case TIMEOUT -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.timeout"));
                case CANCEL -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.cancel"));
                case ERROR -> RequisitionBus.broadcast(MewEconomy.plugin.message("command.requisition.end.error"));
            }
        }).bindWith(consumer);

        Events.subscribe(RequisitionSellEvent.class).handler(e -> {
            // listen to sell event

            if (!cooldownMap.test(e.getSeller().getUniqueId())) {
                RequisitionBus.sendMessage(e.getSeller(), MewEconomy.plugin.message(e.getSeller(), "command.requisition.seller.too-fast"));
                e.setCancelled(true);
                return;
            }

            RequisitionBus.broadcast(Component.text(MewEconomy.plugin.message(e.getSeller(), "command.requisition.sell"))
                    .replaceText(builder -> builder.matchLiteral("{player}").replacement(e.getSeller().displayName()))
                    .replaceText(builder -> builder.matchLiteral("{item}").replacement(e.getRequisition().getReqItem().displayName()))
                    .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(e.getItemToSell().getAmount())))
                    .replaceText(builder -> builder.matchLiteral("{remains}").replacement(Component.text(e.getRequisition().getRemains(e.getItemToSell().getAmount())).color(NamedTextColor.RED))));
        }).bindWith(consumer);
    }
}
