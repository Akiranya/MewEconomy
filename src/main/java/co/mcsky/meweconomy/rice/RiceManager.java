package co.mcsky.meweconomy.rice;

import co.aikar.commands.ACFBukkitUtil;
import co.mcsky.meweconomy.LuckPermsUtil;
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

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.regex.Pattern;

import static co.mcsky.meweconomy.MewEconomy.plugin;

public class RiceManager implements TerminableModule {

    private static final String ESS_PER_WARP_PERM_PREFIX = "essentials.warps.";

    private final IEssentials ess;
    private final LuckPermsUtil lp;

    public RiceManager() {
        this.ess = (net.ess3.api.IEssentials) plugin.getServer().getPluginManager().getPlugin("Essentials");
        this.lp = new LuckPermsUtil();
    }

    /**
     * @param p    the player issuing this command
     * @param name the warp name to set
     */
    public void setWarpCommand(Player p, String name) {
        if (NumberUtil.isInt(name)) {
            p.sendMessage(MewEconomy.plugin.getMessage(p, "command.setwarp.invalid-name", "name", name));
            return;
        }

        final IWarps warps = ess.getWarps();
        Location warpLoc = null;

        try {
            warpLoc = warps.getWarp(name);
        } catch (final WarpNotFoundException | InvalidWorldException ignored) {
        }

        final User iu = ess.getUser(p);
        if (warpLoc == null || name.equalsIgnoreCase(p.getName()) || iu.isAuthorized("essentials.warp.overwrite." + StringUtil.safeString(name))) {
            Date expiryDate = iu.getCommandCooldownExpiry("setwarp");
            if (expiryDate != null && expiryDate.after(new Date())) {
                final Instant expireInstant = expiryDate.toInstant();
                final Instant now = Instant.now();
                final long l = Duration.between(now, expireInstant).toHours();
                p.sendMessage(plugin.getMessage(p, "command.setwarp.cooldown", "remaining", l));
                return;
            }
            try {
                warps.setWarp(iu, name, iu.getLocation());

                // send success message
                final Date expiresAt = Date.from(Instant.now().plus(plugin.config.vip_set_warp_cooldown, ChronoUnit.MILLIS));
                iu.addCommandCooldown(Pattern.compile("^setwarp"), expiresAt, true);
                p.sendMessage(plugin.getMessage(p, "command.setwarp.success", "name", name, "location", ACFBukkitUtil.blockLocationToString(p.getLocation())));

                // add shared permission
                addSharedWarpPermission(name);
            } catch (Exception ignored) {
            }
        } else {
            p.sendMessage(plugin.getMessage(p, "command.setwarp.overwrite"));
        }
    }

    @Override
    public void setup(@NotNull TerminableConsumer consumer) {
        // 如果玩家是会员
        // 在加入游戏时需要向共享权限组添加传送点的权限
        // 在退出游戏时从共享权限组中移除传送点的权限
        Events.subscribe(PlayerJoinEvent.class)
                .filter(e -> plugin.config.vip_enabled)
                .filter(e -> isPlayerVip(e.getPlayer()))
                .handler(e -> addSharedWarpPermission(e.getPlayer().getName().toLowerCase()))
                .bindWith(consumer);
        Events.subscribe(PlayerQuitEvent.class)
                .filter(e -> plugin.config.vip_enabled)
                .filter(e -> isPlayerVip(e.getPlayer()))
                .handler(e -> removeSharedWarpPermission(e.getPlayer().getName().toLowerCase()))
                .bindWith(consumer);
    }

    private boolean isPlayerVip(Player player) {
        return lp.isPlayerInGroup(player, plugin.config.vip_group_name);
    }

    private void addSharedWarpPermission(String vipPlayerName) {
        lp.groupAddPermissionAsync(plugin.config.vip_shared_warp_group_name, ESS_PER_WARP_PERM_PREFIX + vipPlayerName);
    }

    private void removeSharedWarpPermission(String vipPlayerName) {
        lp.groupRemovePermissionAsync(plugin.config.vip_shared_warp_group_name, ESS_PER_WARP_PERM_PREFIX + vipPlayerName);
    }

}
