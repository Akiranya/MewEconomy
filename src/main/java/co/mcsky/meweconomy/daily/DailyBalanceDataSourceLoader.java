package co.mcsky.meweconomy.daily;

import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.meweconomy.serializer.CooldownSerializer;
import co.mcsky.meweconomy.serializer.DailyBalanceDataSourceSerializer;
import co.mcsky.meweconomy.serializer.DailyBalanceModelSerializer;
import com.google.common.reflect.TypeToken;
import me.lucko.helper.config.ConfigFactory;
import me.lucko.helper.cooldown.Cooldown;
import me.lucko.helper.serialize.FileStorageHandler;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

public class DailyBalanceDataSourceLoader extends FileStorageHandler<DailyBalanceDataSource> {

    private final static String fileName = "data";
    private final static String fileExt = ".yml";
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public DailyBalanceDataSourceLoader() {
        super(fileName, fileExt, MewEconomy.plugin.getDataFolder());

        TypeSerializerCollection serializers = TypeSerializerCollection.builder()
                .register(DailyBalanceDataSource.class, new DailyBalanceDataSourceSerializer())
                .register(DailyBalanceModel.class, new DailyBalanceModelSerializer())
                .register(Cooldown.class, new CooldownSerializer())
                .build();
        loader = YamlConfigurationLoader.builder()
                .path(new File(MewEconomy.plugin.getDataFolder(), fileName + fileExt).toPath())
                .defaultOptions(opts -> opts.serializers(builder -> builder.registerAll(serializers)))
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();
        try {
            root = loader.load();
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

    @Override protected DailyBalanceDataSource readFromFile(Path path) {
        try {
            return loader.load().get(DailyBalanceDataSource.class);
        } catch (ConfigurateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override protected void saveToFile(Path path, DailyBalanceDataSource dataSource) {
        try {
            loader.save(root.set(dataSource));
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

}
