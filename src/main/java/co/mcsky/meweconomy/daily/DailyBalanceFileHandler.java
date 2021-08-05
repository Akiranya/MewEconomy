package co.mcsky.meweconomy.daily;

import co.mcsky.meweconomy.serializer.DailyBalanceDatasourceSerializer;
import co.mcsky.meweconomy.serializer.DailyBalanceModelSerializer;
import co.mcsky.moecore.config.YamlConfigFactory;
import me.lucko.helper.serialize.FileStorageHandler;
import org.spongepowered.configurate.CommentedConfigurationNode;
import org.spongepowered.configurate.ConfigurateException;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import java.io.File;
import java.nio.file.Path;

public class DailyBalanceFileHandler extends FileStorageHandler<DailyBalanceDatasource> {

    private final static String fileName = "data";
    private final static String fileExt = ".yml";
    private final YamlConfigurationLoader loader;
    private CommentedConfigurationNode root;

    public DailyBalanceFileHandler(File dataFolder) {
        super(fileName, fileExt, dataFolder);
        TypeSerializerCollection serializers = YamlConfigFactory.typeSerializers().childBuilder()
                .register(DailyBalanceDatasource.class, new DailyBalanceDatasourceSerializer())
                .register(DailyBalanceModel.class, new DailyBalanceModelSerializer())
                .build();
        loader = YamlConfigurationLoader.builder()
                .file(new File(dataFolder, fileName + fileExt))
                .defaultOptions(opt -> opt.serializers(serializers))
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .build();
        try {
            root = loader.load();
        } catch (ConfigurateException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected DailyBalanceDatasource readFromFile(Path path) {
        try {
            return (root = loader.load()).get(DailyBalanceDatasource.class);
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
