package co.mcsky.meweconomy.daily;

import co.mcsky.meweconomy.MewEconomy;
import co.mcsky.meweconomy.serializer.DailyBalanceDataSourceSerializer;
import co.mcsky.meweconomy.serializer.DailyBalanceModelSerializer;
import co.mcsky.moecore.config.YamlConfigFactory;
import me.lucko.helper.serialize.FileStorageHandler;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

public class DailyBalanceFileHandler extends FileStorageHandler<DailyBalanceDatasource> {

    private final static String fileName = "data";
    private final static String fileExt = ".yml";
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public DailyBalanceFileHandler() {
        super(fileName, fileExt, MewEconomy.plugin.getDataFolder());
        TypeSerializerCollection s = YamlConfigFactory.typeSerializers().childBuilder()
                .register(DailyBalanceModel.class, new DailyBalanceModelSerializer())
                .register(DailyBalanceDatasource.class, new DailyBalanceDataSourceSerializer())
                .build();
        loader = YamlConfigFactory.loader(new File(MewEconomy.plugin.getDataFolder(), fileName + fileExt));
        try {
            root = loader.load(loader.defaultOptions().serializers(s));
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected DailyBalanceDatasource readFromFile(Path path) {
        try {
            return root.get(DailyBalanceDatasource.class);
        } catch (ConfigurateException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    protected void saveToFile(Path path, DailyBalanceDatasource dataSource) {
        try {
            loader.save(root.set(dataSource));
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

}
