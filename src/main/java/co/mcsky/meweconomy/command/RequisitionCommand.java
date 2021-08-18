package co.mcsky.meweconomy.command;

import co.aikar.commands.BaseCommand;
import co.aikar.commands.annotation.*;
import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.meweconomy.requisition.Requisition;
import co.mcsky.meweconomy.requisition.RequisitionBus;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

@CommandAlias("%main")
public class RequisitionCommand extends BaseCommand {

    @Subcommand("req")
    @CommandAlias("req")
    @CommandPermission("meco.req")
    @CommandCompletion("@nothing @nothing @materials")
    @Syntax("<数量> <单价> [物品名]")
    public void requisite(Player buyer, int amount, double unitPrice, @Optional String itemName) {

        // stop if there is already a running requisition
        if (RequisitionBus.hasRequisition()) {
            buyer.sendMessage(MewEconomy.plugin.message(buyer, "command.requisition.buyer.already-running"));
            return;
        }

        // if the player is holding an item, requisite the held one.
        // otherwise, use the one specified in the param "itemName".
        ItemStack itemInMainHand = buyer.getInventory().getItemInMainHand();
        if (itemName != null && !itemName.isEmpty()) {
            Material material = Material.matchMaterial(itemName);
            if (material == null || !material.isItem()) {
                buyer.sendMessage(MewEconomy.plugin.message(buyer, "command.requisition.invalid-item"));
                return;
            }
            itemInMainHand = new ItemStack(material);
        }

        if (itemInMainHand.getType().isAir()) {
            buyer.sendMessage(MewEconomy.plugin.message(buyer, "command.requisition.no-item"));
            return;
        }

        itemInMainHand = itemInMainHand.clone();
        RequisitionBus.startRequisition(new Requisition(buyer, itemInMainHand, amount, unitPrice));
    }

    @Subcommand("sell")
    @CommandAlias("msell")
    @CommandCompletion("@nothing")
    @CommandPermission("meco.req")
    @Syntax("<数量>")
    public void sell(Player seller, @Optional Integer amount) {

        // Here we just check command parameters on the seller side

        ItemStack itemInMainHand = seller.getInventory().getItemInMainHand();
        int amountToSell;
        if (amount == null) {
            // if amount not specified, sell whole stack in hand
            amountToSell = itemInMainHand.getAmount();
        } else {
            // otherwise, take the specified amount
            if (amount <= 0) {
                seller.sendMessage(MewEconomy.plugin.message(seller, "command.requisition.invalid-amount"));
                return;
            } else {
                // seller cant sell more than 64 at a time
                // TODO over 64?
                amountToSell = Math.min(amount, 64);
            }
        }

        if (itemInMainHand.getType().isAir()) {
            seller.sendMessage(MewEconomy.plugin.message(seller, "command.requisition.no-item"));
            return;
        }

        if (itemInMainHand.getAmount() < amountToSell) {
            seller.sendMessage(MewEconomy.plugin.message(seller, "command.requisition.seller.insufficient-amount"));
            return;
        }

        ItemStack clone = itemInMainHand.clone().asQuantity(amountToSell);
        RequisitionBus.onSell(seller, clone);
    }
}
