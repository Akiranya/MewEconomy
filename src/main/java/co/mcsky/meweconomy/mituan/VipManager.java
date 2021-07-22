package co.mcsky.meweconomy.mituan;

import co.mcsky.meweconomy.MewEconomy;
import com.earth2me.essentials.IEssentials;
import com.earth2me.essentials.User;
import com.earth2me.essentials.api.IWarps;
import com.earth2me.essentials.commands.WarpNotFoundException;
import com.earth2me.essentials.utils.NumberUtil;
import com.earth2me.essentials.utils.StringUtil;
import me.lucko.helper.Events;
import me.lucko.helper.terminable.TerminableConsumer;
import me.lucko.helper.terminable.module.TerminableModule;
import net.ess3.api.InvalidWorldException;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import static co.mcsky.meweconomy.MewEconomy.plugin;

public class VipManager implements TerminableModule {

    private final IEssentials ess;

    public VipManager() {
        this.ess = (net.ess3.api.IEssentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
    }

    public void setWarp(Player player, String name) {
        if (NumberUtil.isInt(name)) {
            player.sendMessage(MewEconomy.plugin.getMessage(player, "command.setwarp.invalid-name", "name", name));
            return;
        }

        final IWarps warps = ess.getWarps();
        Location warpLoc = null;

        try {
            warpLoc = warps.getWarp(name);
        } catch (final WarpNotFoundException | InvalidWorldException ignored) {
        }

        name = name.toLowerCase(); // force lowercase
        final User iu = ess.getUser(player);
        if (warpLoc == null || iu.isAuthorized("essentials.warp.overwrite." + StringUtil.safeString(name))) {
            try {
                warps.setWarp(iu, name, iu.getLocation());
            } catch (Exception ignored) {
            }
        } else {
            player.sendMessage(plugin.getMessage(player, "command.setwarp.overwrite"));
            return;
        }

        // setting warp succeeds
        player.sendMessage(plugin.getMessage(player, "command.setwarp.success"));
        // add shared permission
        String perm = "essentials.warps." + name;
        final boolean success = plugin.getPerm().groupAdd((String) null, plugin.config.vip_shared_warp_group_name, perm);
        if (plugin.isDebugMode()) {
            if (success) plugin.getLogger().info("Shared permission added: " + perm);
            else plugin.getLogger().severe("Failed to add shared permission: " + perm);
        }
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        if (!plugin.config.vip_enabled) return;

        // 如果玩家是会员
        // 在加入游戏时需要向共享权限组添加传送点的权限
        // 在退出游戏时从共享权限组中移除传送点的权限
        final String essPerWarpPermPrefix = "essentials.warps.";
        Events.subscribe(PlayerJoinEvent.class)
                .filter(e -> plugin.getPerm().playerInGroup(e.getPlayer(), plugin.config.vip_group_name))
                .handler(e -> plugin.getPerm().groupAdd((String) null, plugin.config.vip_shared_warp_group_name, essPerWarpPermPrefix + e.getPlayer().getName().toLowerCase()))
                .bindWith(consumer);
        Events.subscribe(PlayerQuitEvent.class)
                .filter(e -> plugin.getPerm().playerInGroup(e.getPlayer(), plugin.config.vip_group_name))
                .handler(e -> plugin.getPerm().groupRemove((String) null, plugin.config.vip_shared_warp_group_name, essPerWarpPermPrefix + e.getPlayer().getName().toLowerCase()))
                .bindWith(consumer);
    }
}
