package co.mcsky.meweconomy.config;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.*;
import me.lucko.helper.gson.converter.GsonConverters;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.spongepowered.configurate.ConfigurationNode;
import org.spongepowered.configurate.serialize.SerializationException;
import org.spongepowered.configurate.serialize.TypeSerializer;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class GsonTypeSerializer implements TypeSerializer<JsonElement> {
    public static final Class<JsonElement> TYPE = JsonElement.class;
    public static final GsonTypeSerializer INSTANCE = new GsonTypeSerializer();

    @Override
    public JsonElement deserialize(Type type, ConfigurationNode from) throws SerializationException {
        if (from.raw() == null) {
            return JsonNull.INSTANCE;
        }

        if (from.isList()) {
            List<? extends ConfigurationNode> childrenList = from.childrenList();
            JsonArray array = new JsonArray();
            for (ConfigurationNode node : childrenList) {
                array.add(node.get(TYPE));
            }
            return array;
        }

        if (from.isMap()) {
            Map<Object, ? extends ConfigurationNode> childrenMap = from.childrenMap();
            JsonObject object = new JsonObject();
            for (Map.Entry<Object, ? extends ConfigurationNode> ent : childrenMap.entrySet()) {
                object.add(ent.getKey().toString(), ent.getValue().get(TYPE));
            }
            return object;
        }

        Object val = from.raw();
        try {
            return GsonConverters.IMMUTABLE.wrap(val);
        } catch (IllegalArgumentException e) {
            throw new SerializationException(e);
        }
    }

    @Override
    public void serialize(Type type, @Nullable JsonElement from, ConfigurationNode to) throws SerializationException {

        if (from.isJsonPrimitive()) {
            JsonPrimitive primitive = from.getAsJsonPrimitive();
            to.set(GsonConverters.IMMUTABLE.unwarpPrimitive(primitive));
        } else if (from.isJsonNull()) {
            to.set(null);
        } else if (from.isJsonArray()) {
            JsonArray array = from.getAsJsonArray();
            // ensure 'to' is a list node
            to.set(ImmutableList.of());
            for (JsonElement element : array) {
                serialize(TYPE, element, to.appendListNode());
            }
        } else if (from.isJsonObject()) {
            JsonObject object = from.getAsJsonObject();
            // ensure 'to' is a map node
            to.set(ImmutableMap.of());
            for (Map.Entry<String, JsonElement> ent : object.entrySet()) {
                serialize(TYPE, ent.getValue(), to.node(ent.getKey()));
            }
        } else {
            throw new SerializationException("Unknown element type: " + from.getClass());
        }
    }
}
