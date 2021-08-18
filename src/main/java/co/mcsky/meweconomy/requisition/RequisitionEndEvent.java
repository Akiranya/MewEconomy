package co.mcsky.meweconomy.requisition;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RequisitionEndEvent extends Event {

    public static final HandlerList handlers = new HandlerList();

    private final Requisition requisition;

    public RequisitionEndEvent(Requisition requisition) {
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
