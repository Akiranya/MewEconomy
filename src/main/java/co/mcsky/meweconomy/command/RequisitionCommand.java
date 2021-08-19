package co.mcsky.meweconomy.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.meweconomy.requisition.EndReason;
import co.mcsky.meweconomy.requisition.Requisition;
import co.mcsky.meweconomy.requisition.RequisitionBus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("%main")
public class RequisitionCommand extends BaseCommand {

    @Subcommand("req cancel")
    @CommandPermission("meco.req")
    @Syntax("<数量> <单价> [物品名]")
    public void cancel(Player player) {
        if (!RequisitionBus.INSTANCE.hasRequisition()) {
            RequisitionBus.sendMessage(player, MewEconomy.plugin.message("command.requisition.seller.no-requisition"));
            return;
        }
        if (player.hasPermission("meco.admin") || player.getUniqueId().equals(RequisitionBus.INSTANCE.currentRequisition().getBuyer().getUniqueId())) {
            RequisitionBus.INSTANCE.stopRequisition(EndReason.CANCEL);
        } else {
            RequisitionBus.sendMessage(player, MewEconomy.plugin.message(player, "command.requisition.cancel-failed"));
        }
    }

    @Subcommand("req")
    @CommandAlias("req")
    @CommandPermission("meco.req")
    @CommandCompletion("@nothing @nothing @materials")
    @Syntax("<数量> <单价> [物品名]")
    public void requisite(Player buyer, int amount, double unitPrice, @Optional String itemName) {

        // stop if there is already a running requisition
        if (RequisitionBus.INSTANCE.hasRequisition()) {
            RequisitionBus.sendMessage(buyer, MewEconomy.plugin.message("command.requisition.buyer.already-running"));
            return;
        }

        // start check the command params of buyer side

        // if the player is holding an item, requisite the held one.
        // otherwise, use the one specified in the param "itemName".
        ItemStack itemInMainHand = buyer.getInventory().getItemInMainHand();
        if (itemName != null && !itemName.isEmpty()) {
            Material material = Material.matchMaterial(itemName);
            if (material == null || !material.isItem()) {
                RequisitionBus.sendMessage(buyer, MewEconomy.plugin.message("command.requisition.invalid-item"));
                return;
            }
            itemInMainHand = new ItemStack(material);
        }

        if (itemInMainHand.getType().isAir()) {
            RequisitionBus.sendMessage(buyer, MewEconomy.plugin.message("command.requisition.no-item"));
            return;
        }

        itemInMainHand = itemInMainHand.clone();
        RequisitionBus.INSTANCE.startRequisition(new Requisition(buyer, itemInMainHand, amount, unitPrice));
    }

    @Subcommand("sell")
    @CommandAlias("msell")
    @CommandCompletion("@nothing")
    @CommandPermission("meco.req")
    @Syntax("<数量>")
    public void sell(Player seller, @Optional Integer amount) {

        // there is no running requisition
        if (!RequisitionBus.INSTANCE.hasRequisition()) {
            RequisitionBus.sendMessage(seller, MewEconomy.plugin.message("command.requisition.seller.no-requisition"));
            return;
        }

        // Here we just check command parameters on the seller side

        ItemStack itemInMainHand = seller.getInventory().getItemInMainHand();
        if (itemInMainHand.getType().isAir()) {
            RequisitionBus.sendMessage(seller, MewEconomy.plugin.message("command.requisition.no-item"));
            return;
        }

        int amountToSell;
        if (amount == null) {
            // if amount not specified, sell whole stack in hand
            amountToSell = itemInMainHand.getAmount();
        } else {
            // otherwise, take the specified amount
            if (amount <= 0) {
                RequisitionBus.sendMessage(seller, MewEconomy.plugin.message("command.requisition.invalid-amount"));
                return;
            } else {
                // seller cant sell more than 64 at a time
                amountToSell = amount;
            }
        }

        if (!seller.getInventory().containsAtLeast(itemInMainHand, amountToSell)) {
            RequisitionBus.sendMessage(seller, MewEconomy.plugin.message(seller, "command.requisition.seller.insufficient-amount"));
            return;
        }

        ItemStack clone = itemInMainHand.asQuantity(amountToSell); // could be over 64
        RequisitionBus.INSTANCE.onSell(seller, clone);
    }
}
