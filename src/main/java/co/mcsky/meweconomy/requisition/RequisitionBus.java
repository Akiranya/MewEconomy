package co.mcsky.meweconomy.requisition;

import co.mcsky.meweconomy.MewEconomy;
import me.lucko.helper.Events;
import me.lucko.helper.Schedulers;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import me.lucko.helper.utils.Players;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;

/**
 * Manages the lifetime of requisition.
 */
public class RequisitionBus implements TerminableModule {

    private static Requisition currentRequisition;
    private static int taskId;

    public static void startRequisition(Requisition req) {
        currentRequisition = req;
        taskId = Schedulers.sync().runRepeating(new RequisitionTask(req), 1, 20).getBukkitId();
        broadcast(Component.text(MewEconomy.plugin.message("command.requisition.req-init"))
                .replaceText(builder -> builder.matchLiteral("{player}").replacement(currentRequisition.getBuyer().displayName()))
                .replaceText(builder -> builder.matchLiteral("{item}").replacement(currentRequisition.getReqItem().displayName()))
                .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(currentRequisition.getRemains())))
                .replaceText(builder -> builder.matchLiteral("{unit_price}").replacement(Component.text(currentRequisition.getUnitPrice()).color(NamedTextColor.LIGHT_PURPLE)))
                .replaceText(builder -> builder.matchLiteral("{remaining_time}").replacement(Component.text(currentRequisition.getDuration()))));
    }

    public static void stopRequisition() {
        currentRequisition = null;
        Schedulers.bukkit().cancelTask(taskId);
        broadcast(MewEconomy.plugin.message("command.requisition.stop"));
    }

    public static Requisition currentRequisition() {
        return currentRequisition;
    }

    public static boolean hasRequisition() {
        return currentRequisition != null;
    }

    public static void broadcast(String message) {
        broadcast(Component.text(message));
    }

    public static void broadcast(Component message) {
        // TODO limit broadcast freq
        Bukkit.broadcast(Component.text(MewEconomy.plugin.message("command.requisition.prefix")).append(message));
    }

    public static void giveItem(OfflinePlayer player, ItemStack itemStack) {
        final Optional<Player> opt = Players.get(player.getUniqueId());
        if (opt.isPresent()) {
            final Player p = opt.get();
            // try to add items to the inventory
            final HashMap<Integer, ItemStack> leftover = p.getInventory().addItem(itemStack);
            // any items cant fit in the inventory will be dropped on the buyer's feet
            leftover.forEach((i, is) -> p.getWorld().dropItemNaturally(p.getLocation(), is));
        } else {
            // if the buyer currently offline, simply drop items on the location where he last logged out
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
    public static boolean matched(ItemStack test) {
        // TODO better implementation?
        return currentRequisition.getReqItem().isSimilar(test);
    }

    /**
     * Called when some player sells items to this requisition.
     *
     * @param seller     the seller
     * @param itemToSell the item to sell
     */
    public static void onSell(Player seller, ItemStack itemToSell) {

        // there is no running requisition
        if (!hasRequisition()) {
            seller.sendMessage(MewEconomy.plugin.message(seller, "command.requisition.seller.no-requisition"));
            return;
        }

        // the item to be sold does not match
        if (!matched(itemToSell)) {
            seller.sendMessage(MewEconomy.plugin.message(seller, "command.requisition.seller.invalid-item"));
            return;
        }

        // oversold
        int remains = currentRequisition().getRemains();
        if (itemToSell.getAmount() > remains) {
            seller.sendMessage(Component.text(MewEconomy.plugin.message(seller, "command.requisition.seller.oversold"))
                    .replaceText(b -> b.matchLiteral("{needed}").replacement(
                            Component.text(currentRequisition.getRemains())
                                    .append(Component.text("x").color(NamedTextColor.GRAY))
                                    .append(currentRequisition.getReqItem().displayName())))
                    .replaceText(b -> b.matchLiteral("{actual_amount}").replacement(Component.text(itemToSell.getAmount()))));
            return;
        }

        // buyer cant afford the items
        final double price = itemToSell.getAmount() * currentRequisition().getUnitPrice();
        if (!MewEconomy.plugin.economy().has(currentRequisition().getBuyer(), price)) {
            seller.sendMessage(MewEconomy.plugin.message(seller, "command.requisition.seller.insufficient-fund"));
            return;
        }

        // all check passed, process the trade //

        final ItemStack backup = seller.getInventory().getItemInMainHand().clone();
        final ItemStack sellerHandItemCopy = seller.getInventory().getItemInMainHand().clone();

        try {
            sellerHandItemCopy.setAmount(Math.max(sellerHandItemCopy.getAmount() - itemToSell.getAmount(), 0));

            // remove certain amount of items from the seller's main hand
            if (sellerHandItemCopy.getAmount() == 0) {
                seller.getInventory().setItemInMainHand(null); // prevent bugs
            } else {
                seller.getInventory().setItemInMainHand(sellerHandItemCopy);
            }

        } catch (Exception e) {
            MewEconomy.plugin.getLogger().log(Level.SEVERE, "Error selling item: ", e);
            seller.getInventory().setItemInMainHand(backup); // rollback
            return;
        }

        // give money to the seller
        MewEconomy.plugin.economy().depositPlayer(seller, price);

        // give item to the buyer
        giveItem(currentRequisition().getBuyer(), itemToSell);
        MewEconomy.plugin.economy().withdrawPlayer(currentRequisition().getBuyer(), price);

        // update requisition state: increment amount sold
        currentRequisition().incrementAmountSold(itemToSell.getAmount());

        broadcast(Component.text(MewEconomy.plugin.message(seller, "command.requisition.sell"))
                .replaceText(builder -> builder.matchLiteral("{player}").replacement(seller.displayName()))
                .replaceText(builder -> builder.matchLiteral("{item}").replacement(currentRequisition.getReqItem().displayName()))
                .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(itemToSell.getAmount())))
                .replaceText(builder -> builder.matchLiteral("{remains}").replacement(Component.text(currentRequisition.getRemains()).color(NamedTextColor.RED))));

        // halt the requisition if amount sold is enough
        if (currentRequisition.getRemains() <= 0) {
            stopRequisition();
        }
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        // update buyer's location if logging out for the purpose of dropping items
        Events.subscribe(PlayerQuitEvent.class)
                .filter(e -> currentRequisition() != null)
                .filter(e -> e.getPlayer().equals(currentRequisition().getBuyer()))
                .handler(e -> currentRequisition().setBuyerLocation(e.getPlayer().getLocation()))
                .bindWith(consumer);

    }

    /**
     * This task must be scheduled to run EVERY second.
     */
    private static class RequisitionTask extends BukkitRunnable {

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
                    stopRequisition();
                    return;
                }

                if (broadcastTimes.contains(--remainingSeconds)) {
                    // broadcast requisition at certain times
                    broadcast(MewEconomy.plugin.message("command.requisition.remaining", "time", remainingSeconds));
                } else if (remainingSeconds > 0 && remainingSeconds % MewEconomy.plugin.config.broadcast_interval == 0) {
                    // broadcast requisition at certain interval
                    broadcast(Component.text(MewEconomy.plugin.message("command.requisition.req-update"))
                            .replaceText(builder -> builder.matchLiteral("{player}").replacement(requisition.getBuyer().displayName()))
                            .replaceText(builder -> builder.matchLiteral("{item}").replacement(requisition.getReqItem().displayName()))
                            .replaceText(builder -> builder.matchLiteral("{amount}").replacement(Component.text(requisition.getAmountNeeded())))
                            .replaceText(builder -> builder.matchLiteral("{unit_price}").replacement(Component.text(requisition.getUnitPrice()).color(NamedTextColor.LIGHT_PURPLE)))
                            .replaceText(builder -> builder.matchLiteral("{remains}").replacement(Component.text(requisition.getRemains()).color(NamedTextColor.RED))));
                }

            } catch (Exception e) {
                e.printStackTrace();
                stopRequisition();
            }
        }

    }
}
