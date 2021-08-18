package co.mcsky.meweconomy.requisition;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerEvent;
import org.jetbrains.annotations.NotNull;

public class RequisitionSellEvent extends PlayerEvent {

    public static final HandlerList handlers = new HandlerList();
    private final Requisition requisition;

    public RequisitionSellEvent(@NotNull Player seller, Requisition requisition) {
        super(seller);
        this.requisition = requisition;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Requisition getRequisition() {
        return requisition;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

}
