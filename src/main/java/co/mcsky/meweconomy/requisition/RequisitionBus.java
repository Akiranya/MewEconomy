package co.mcsky.meweconomy.requisition;

import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.meweconomy.requisition.event.RequisitionEndEvent;
import co.mcsky.meweconomy.requisition.event.RequisitionSellEvent;
import co.mcsky.meweconomy.requisition.event.RequisitionStartEvent;
import me.lucko.helper.Events;
import me.lucko.helper.Schedulers;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.composite.CompositeTerminable;
import me.lucko.helper.terminable.module.TerminableModule;
import me.lucko.helper.utils.Players;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

/**
 * Manages the lifetime of requisition.
 */
public enum RequisitionBus implements TerminableModule, TerminableConsumer {

    INSTANCE;

    // manages the termination of current requisition
    private final CompositeTerminable requisitionRegistry = CompositeTerminable.createWeak();

    private Requisition currentRequisition;
    private int requisitionTaskId;

    public static void broadcast(String message) {
        broadcast(Component.text(message));
    }

    public static void broadcast(Component message) {
        Bukkit.broadcast(Component.text(MewEconomy.plugin.message("command.requisition.prefix")).append(message));
    }

    public static void sendMessage(CommandSender sender, Component message) {
        sender.sendMessage(Component.text(MewEconomy.plugin.message("command.requisition.prefix")).append(message));
    }

    public static void sendMessage(CommandSender sender, String message) {
        sendMessage(sender, Component.text(message));
    }

    public void startRequisition(Requisition req) {
        if (Events.callAndReturn(new RequisitionStartEvent(req)).isCancelled())
            return;

        currentRequisition = bindModule(req);
        requisitionTaskId = Schedulers.sync().runRepeating(new RequisitionTask(req), 1, 20).getBukkitId();
    }

    public void stopRequisition(EndReason reason) {
        // no need to stop if there is nothing
        if (!hasRequisition()) return;

        Events.call(new RequisitionEndEvent(currentRequisition, reason));

        currentRequisition = null;
        Schedulers.bukkit().cancelTask(requisitionTaskId);
        requisitionRegistry.closeAndReportException();
    }

    public Requisition currentRequisition() {
        return currentRequisition;
    }

    public boolean hasRequisition() {
        return currentRequisition != null;
    }

    public void giveItem(OfflinePlayer player, ItemStack itemStack) {
        final Optional<Player> opt = Players.get(player.getUniqueId());
        if (opt.isPresent()) {
            final Player p = opt.get();
            // try to add items to the inventory (has side-effect on the input item stack)
            final HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(itemStack);
            // drop the items on the feet if inventory full
            leftover.forEach((i, is) -> p.getWorld().dropItemNaturally(p.getLocation(), is));
        } else {
            // if the player currently offline, simply drop items on the location where he last logged out
            final Location buyerLocation = currentRequisition().getBuyerLocation();
            buyerLocation.getWorld().dropItem(buyerLocation, itemStack);
        }
    }

    /**
     * Checks whether the specified item is equivalent to this item.
     *
     * @param test the item
     * @return true if the test item is equivalent to this item
     */
    public boolean matched(ItemStack test) {
        // TODO more reasonable implementation to compare items
        return currentRequisition.getReqItem().isSimilar(test);
    }

    /**
     * Called when some player sells items to this requisition.
     *
     * @param seller     the seller
     * @param itemToSell the item to sell
     */
    public void onSell(Player seller, ItemStack itemToSell) {

        // check conditions

        // sell to self
        if (seller.getUniqueId().equals(currentRequisition.getBuyer().getUniqueId())) {
            RequisitionBus.sendMessage(seller, MewEconomy.plugin.message("command.requisition.seller.sell-to-self"));
            return;
        }

        // item to be sold does not match
        if (!matched(itemToSell)) {
            RequisitionBus.sendMessage(seller, MewEconomy.plugin.message("command.requisition.seller.invalid-item"));
            return;
        }

        // seller oversold
        int remains = currentRequisition().getRemains();
        if (itemToSell.getAmount() > remains) {
            RequisitionBus.sendMessage(seller, Component.text(MewEconomy.plugin.message("command.requisition.seller.oversold"))
                    .replaceText(b -> b.matchLiteral("{needed}").replacement(Component.text(currentRequisition.getRemains())))
                    .replaceText(b -> b.matchLiteral("{actual_amount}").replacement(Component.text(itemToSell.getAmount()))));
            return;
        }

        // calculates the price of this transaction
        final double price = itemToSell.getAmount() * currentRequisition().getUnitPrice();

        // buyer cant afford the items
        if (!MewEconomy.plugin.economy().has(currentRequisition().getBuyer(), price)) {
            RequisitionBus.sendMessage(seller, MewEconomy.plugin.message("command.requisition.seller.insufficient-fund"));
            return;
        }

        if (Events.callAndReturn(new RequisitionSellEvent(currentRequisition, seller, itemToSell)).isCancelled()) {
            return;
        }

        /*
         --- process the seller side ---
        */

        seller.getInventory().removeItemAnySlot(itemToSell);
        MewEconomy.plugin.economy().depositPlayer(seller, price);

        /*
         --- process the buyer side ---

         CAVEAT: it's NECESSARY to pass on a clone of the item because
         method Inventory#addItem() has side-effects on the input items
        */

        giveItem(currentRequisition().getBuyer(), itemToSell.clone());
        MewEconomy.plugin.economy().withdrawPlayer(currentRequisition().getBuyer(), price);

        if (currentRequisition.incrementAmountSold(itemToSell.getAmount()).getRemains() <= 0) {
            stopRequisition(EndReason.AMOUNT_MET);
        }
    }

    @NotNull
    @Override
    public <T extends AutoCloseable> T bind(@NotNull T terminable) {
        return requisitionRegistry.bind(terminable);
    }

    @NotNull
    @Override
    public <T extends TerminableModule> T bindModule(@NotNull T module) {
        return requisitionRegistry.bindModule(module);
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        // bind this with the plugin main instance
        consumer.bind(requisitionRegistry);
        consumer.bindModule(new RequisitionListener());
    }

    /**
     * CAVEAT: this task must be scheduled to run EVERY second.
     */
    private class RequisitionTask extends BukkitRunnable {

        private final Requisition requisition;
        private final Set<Integer> broadcastTimes;
        private int remainingSeconds;

        public RequisitionTask(Requisition requisition) {
            this.requisition = requisition;
            this.broadcastTimes = new HashSet<>(MewEconomy.plugin.config.broadcast_times);
            this.remainingSeconds = requisition.getDuration();
        }

        @Override
        public void run() {
            try {
                if (requisition.isTimeout()) {
                    stopRequisition(EndReason.TIMEOUT);
                    return;
                }
                if (broadcastTimes.contains(--remainingSeconds)) {
                    // broadcast requisition at certain times
                    broadcast(Component.text(MewEconomy.plugin.message("command.requisition.req-update"))
                            .replaceText(builder -> builder.matchLiteral("{player}").replacement(requisition.getBuyer().displayName()))
                            .replaceText(builder -> builder.matchLiteral("{item}").replacement(requisition.getReqItem().displayName()))
                            .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(requisition.getTotalAmountNeeded())))
                            .replaceText(builder -> builder.matchLiteral("{unit_price}").replacement(Component.text(requisition.getUnitPrice()).color(NamedTextColor.LIGHT_PURPLE)))
                            .replaceText(builder -> builder.matchLiteral("{remains}").replacement(Component.text(requisition.getRemains()).color(NamedTextColor.RED)))
                            .replaceText(builder -> builder.matchLiteral("{remaining_time}").replacement(Component.text(remainingSeconds))));
                }
            } catch (Exception e) {
                e.printStackTrace();
                stopRequisition(EndReason.ERROR);
            }
        }

    }
}
