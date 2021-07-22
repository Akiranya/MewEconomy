package co.mcsky.meweconomy.config;

import com.google.gson.JsonElement;
import me.lucko.helper.config.ConfigFactory;
import me.lucko.helper.gson.GsonSerializable;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.configurate.ConfigurationOptions;
import org.spongepowered.configurate.serialize.TypeSerializerCollection;
import org.spongepowered.configurate.yaml.NodeStyle;
import org.spongepowered.configurate.yaml.YamlConfigurationLoader;

import javax.annotation.Nonnull;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public abstract class YamlConfigFactory {

    private static final TypeSerializerCollection TYPE_SERIALIZERS;

    static {
        TYPE_SERIALIZERS = TypeSerializerCollection.defaults().childBuilder()
                .register(JsonElement.class, GsonTypeSerializer.INSTANCE)
                .register(GsonSerializable.class, HelperTypeSerializer.INSTANCE)
                .register(ConfigurationSerializable.class, BukkitTypeSerializer.INSTANCE)
                .register(String.class, ColoredStringTypeSerializer.INSTANCE)
                .register(Component.class, Text3TypeSerializer.INSTANCE)
                .build();
    }

    public static YamlConfigurationLoader loader(@Nonnull Path path) {
        return YamlConfigurationLoader.builder()
                .nodeStyle(NodeStyle.BLOCK)
                .indent(2)
                .source(() -> Files.newBufferedReader(path, StandardCharsets.UTF_8))
                .sink(() -> Files.newBufferedWriter(path, StandardCharsets.UTF_8))
                .defaultOptions(configurationOptions -> ConfigurationOptions.defaults().serializers(TYPE_SERIALIZERS))
                .build();
    }

    public static YamlConfigurationLoader loader(@Nonnull File file) {
        return loader(file.toPath());
    }

}
