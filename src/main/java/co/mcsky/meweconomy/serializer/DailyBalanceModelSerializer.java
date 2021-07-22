package co.mcsky.meweconomy.serializer;

import co.mcsky.meweconomy.daily.DailyBalanceModel;
import com.google.common.base.Preconditions;
import me.lucko.helper.cooldown.Cooldown;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.Objects;
import java.util.UUID;

public class DailyBalanceModelSerializer implements TypeSerializer<DailyBalanceModel> {
    @Override
    public DailyBalanceModel deserialize(Type type, ConfigurationNode node) throws SerializationException {
        UUID uuid = node.node("uuid").get(UUID.class);
        double balance = node.node("daily-balance").getDouble();
        Cooldown cooldown = node.node("cooldown").get(Cooldown.class, DailyBalanceModel.getDefaultCooldown());

        Preconditions.checkNotNull(uuid, "uuid is null");
        Preconditions.checkNotNull(cooldown, "cooldown is null");

        return new DailyBalanceModel(uuid, balance, cooldown);
    }

    @Override
    public void serialize(Type type, @Nullable DailyBalanceModel obj, ConfigurationNode node) throws SerializationException {
        Objects.requireNonNull(obj, "obj");

        node.node("uuid").set(UUID.class, obj.getPlayerUUID());
        node.node("daily-balance").set(obj.getDailyBalance());
        node.node("cooldown").set(obj.getCooldown());
    }
}
