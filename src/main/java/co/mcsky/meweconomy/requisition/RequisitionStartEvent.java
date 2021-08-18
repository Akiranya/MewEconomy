package co.mcsky.meweconomy.requisition;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RequisitionStartEvent extends Event {

    public static final HandlerList handlers = new HandlerList();

    private final Requisition requisition;

    public RequisitionStartEvent(Requisition requisition) {
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
