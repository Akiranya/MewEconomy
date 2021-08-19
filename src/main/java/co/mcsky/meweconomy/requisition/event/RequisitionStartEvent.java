package co.mcsky.meweconomy.requisition.event;

import co.mcsky.meweconomy.requisition.Requisition;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class RequisitionStartEvent extends Event implements Cancellable {

    public static final HandlerList handlers = new HandlerList();

    private final Requisition requisition;
    private boolean cancel;

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

    @Override
    public boolean isCancelled() {
        return cancel;
    }

    @Override
    public void setCancelled(boolean cancel) {
        this.cancel = cancel;
    }
}
