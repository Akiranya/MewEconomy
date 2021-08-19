package co.mcsky.meweconomy.requisition.event;

import co.mcsky.meweconomy.requisition.EndReason;
import co.mcsky.meweconomy.requisition.Requisition;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RequisitionEndEvent extends Event {

    public static final HandlerList handlers = new HandlerList();

    private final Requisition requisition;

    private final EndReason endReason;

    public RequisitionEndEvent(Requisition requisition, EndReason reason) {
        this.requisition = requisition;
        this.endReason = reason;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public EndReason getRequisitionEndReason() {
        return endReason;
    }

    public Requisition getRequisition() {
        return requisition;
    }

    @Override
    public @NotNull HandlerList getHandlers() {
        return handlers;
    }

}
