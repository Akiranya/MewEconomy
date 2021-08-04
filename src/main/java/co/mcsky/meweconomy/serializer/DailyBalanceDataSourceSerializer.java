package co.mcsky.meweconomy.serializer;

import co.mcsky.meweconomy.daily.DailyBalanceDatasource;
import co.mcsky.meweconomy.daily.DailyBalanceModel;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class DailyBalanceDataSourceSerializer implements TypeSerializer<DailyBalanceDatasource> {

    @Override
    public DailyBalanceDatasource deserialize(Type type, ConfigurationNode node) throws SerializationException {
        DailyBalanceDatasource dataSource = new DailyBalanceDatasource();
        List<DailyBalanceModel> playerModels = node.node("players").getList(DailyBalanceModel.class, new ArrayList<>());
        dataSource.addPlayerModels(playerModels);
        return dataSource;
    }

    @Override
    public void serialize(Type type, @Nullable DailyBalanceDatasource dataSource, ConfigurationNode node) throws
            SerializationException {
        Objects.requireNonNull(dataSource, "dataSource");
        node.node("version").set(1);
        node.node("players").setList(DailyBalanceModel.class, dataSource.getPlayerModels());
    }

}
