package co.mcsky.meweconomy.requisition.event;

import co.mcsky.meweconomy.requisition.Requisition;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class RequisitionSellEvent extends Event implements Cancellable {

    public static final HandlerList handlers = new HandlerList();
    private final Requisition requisition;
    private final Player seller;

    private final ItemStack itemToSell;

    private boolean cancel;

    public RequisitionSellEvent(@NotNull Requisition requisition, Player seller, ItemStack itemToSell) {
        this.requisition = requisition;
        this.seller = seller;
        this.itemToSell = itemToSell;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public Requisition getRequisition() {
        return requisition;
    }

    public Player getSeller() {
        return seller;
    }

    public ItemStack getItemToSell() {
        return itemToSell;
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
