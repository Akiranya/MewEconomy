package co.mcsky.meweconomy.requisition;

import co.mcsky.meweconomy.MewEconomy;
import me.lucko.helper.Events;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import org.bukkit.Location;
import org.bukkit.block.ShulkerBox;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BlockStateMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

/**
 * Represents the state of a requisition.
 */
public class Requisition implements TerminableModule {

    // duration of the requisition in second
    private final int duration;

    // start time
    private final long startTime;
    // buyer of this requisition
    private final Player buyer;
    // the item to in this requisition, it's here for comparison of type/name/lore
    private final ItemStack reqItem;
    // the amount of this item to buy
    private final int amountNeeded;
    // the unit price of this item
    private final double unitPrice;

    // record how many items have been sold to this requisition
    private int amountSold;
    // buyer's location, used to drop items if the buyer is logout
    private Location buyerLocation;

    public Requisition(Player buyer, ItemStack reqItem, int amountNeeded, double unitPrice) {
        checkShulkerBox(reqItem);

        this.startTime = System.currentTimeMillis();
        this.buyer = buyer;

        this.reqItem = reqItem.asOne(); // set to 1 because we don't need it

        this.amountNeeded = amountNeeded;
        this.unitPrice = unitPrice;
        this.duration = MewEconomy.plugin.config.requisition_duration;
    }

    public int getDuration() {
        return duration;
    }

    public Player getBuyer() {
        return buyer;
    }

    public ItemStack getReqItem() {
        return reqItem;
    }

    public int getAmountNeeded() {
        return amountNeeded;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getAmountSold() {
        return amountSold;
    }

    public void incrementAmountSold(int amountSold) {
        this.amountSold += amountSold;
    }

    public int getRemains() {
        return amountNeeded - amountSold;
    }

    public boolean isTimeout() {
        return TimeUnit.of(ChronoUnit.MILLIS).toSeconds(System.currentTimeMillis() - startTime) >= duration;
    }

    public long getStartTime() {
        return startTime;
    }

    public Location getBuyerLocation() {
        return buyerLocation;
    }

    public void setBuyerLocation(Location buyerLocation) {
        this.buyerLocation = buyerLocation;
    }

    private void checkShulkerBox(ItemStack test) {
        ItemMeta itemMeta = test.getItemMeta();
        if (itemMeta instanceof BlockStateMeta && ((BlockStateMeta) itemMeta).getBlockState() instanceof ShulkerBox blockState) {
            for (ItemStack next : blockState.getInventory()) {
                if (next != null && !next.getType().isAir()) {
                    throw new IllegalArgumentException("shulker box with content is not supported.");
                }
            }
        }
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        // update buyer's location if logging out for the purpose of dropping items
        Events.subscribe(PlayerQuitEvent.class)
                .filter(e -> getBuyer().getUniqueId().equals(e.getPlayer().getUniqueId()))
                .handler(e -> setBuyerLocation(e.getPlayer().getLocation()))
                .bindWith(consumer);
    }

}
