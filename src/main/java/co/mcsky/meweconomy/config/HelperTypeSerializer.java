package co.mcsky.meweconomy.config;

import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import me.lucko.helper.gson.GsonSerializable;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;

public class HelperTypeSerializer implements TypeSerializer<GsonSerializable> {
    private static final Class<JsonElement> JSON_ELEMENT_TYPE = JsonElement.class;
    public static final HelperTypeSerializer INSTANCE = new HelperTypeSerializer();

    private HelperTypeSerializer() {
    }

    @Override
    public GsonSerializable deserialize(Type type, ConfigurationNode node) throws SerializationException {
        return GsonSerializable.deserializeRaw(type.getClass(), node.get(JSON_ELEMENT_TYPE, JsonNull.INSTANCE));
    }


    @Override
    public void serialize(Type type, @Nullable GsonSerializable s, ConfigurationNode node) throws SerializationException {
        node.set(JSON_ELEMENT_TYPE, s.serialize());
    }
}
