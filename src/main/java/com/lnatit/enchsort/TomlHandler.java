package com.lnatit.enchsort;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import org.apache.commons.lang3.text.WordUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.lnatit.enchsort.EnchSort.LOGGER;
import static com.lnatit.enchsort.EnchSort.MOD_ID;

@EventBusSubscriber(Dist.CLIENT)
public class TomlHandler {
    private static File RULE_FILE;
    private static Toml RULE_TOML;

    private static final String FILE_NAME = MOD_ID + "-rule.toml";
    private static final SequenceConfig DEFAULT_CONFIG = new SequenceConfig();
    private static final HashMap<String, SequenceConfig> CONFIG_MAP = new HashMap<>();

    public static SequenceConfig getConfig(final String id) {
        return CONFIG_MAP.getOrDefault(id, DEFAULT_CONFIG);
    }

    @SubscribeEvent
    public static void initializeRules(final TagsUpdatedEvent event) {
        Registry<Enchantment> registry = event.getRegistryAccess().registryOrThrow(Registries.ENCHANTMENT);
        RULE_FILE = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME).toFile();

        try {
            try {
                if (!RULE_FILE.exists() || RULE_FILE.createNewFile()) {
                    writeDefault(registry);
                }

                RULE_TOML = new Toml().read(RULE_FILE);
            } catch (RuntimeException exception) {
                LOGGER.warn(FILE_NAME + " contains invalid toml, file will be re-generated", exception);
                writeDefault(registry);
            }
        } catch (IOException exception) {
            LOGGER.error("Failed to create or write " + FILE_NAME, exception);
        }

        parseRule();
    }

    private static void parseRule() {
        RULE_TOML.entrySet().forEach(entry -> {
            if (entry.getValue() instanceof Toml toml) {
                SequenceConfig config = toml.to(SequenceConfig.class);
                CONFIG_MAP.put(config.name, config);
            }
        });
    }

    private static void writeDefault(final Registry<Enchantment> registry) throws IOException {
        TomlWriter writer = new TomlWriter.Builder()
                .indentValuesBy(2)
                .indentTablesBy(2)
                .build();

        Map<String, Map<String, Object>> defaultSequence = new LinkedHashMap<>();

        registry.keySet().stream().sorted(TomlHandler::compareResource).forEach(location -> {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put(SequenceConfig.NAME, location.toString());
            element.put(SequenceConfig.SEQUENCE, 0);
            element.put(SequenceConfig.CUSTOM_MAX_LEVEL, 0);

            String formatted = location.getNamespace() + " - " + location.getPath();
            formatted = formatted.replace("_", " ");
            defaultSequence.put(WordUtils.capitalize(formatted), element);
        });

        writer.write(defaultSequence, RULE_FILE);
    }

    private static int compareResource(final ResourceLocation first, final ResourceLocation second) {
        if (first.getNamespace().startsWith(ResourceLocation.DEFAULT_NAMESPACE)) {
            return -1;
        }

        if (second.getNamespace().startsWith(ResourceLocation.DEFAULT_NAMESPACE)) {
            return 1;
        }

        if (!first.getNamespace().equals(second.getNamespace())) {
            return first.getNamespace().compareTo(second.getNamespace());
        }

        return first.getPath().compareTo(second.getPath());
    }

    public static class SequenceConfig {
        private final String name;
        private final int sequence;
        private final int customMaxLevel;

        public static final String NAME = "name";
        public static final String SEQUENCE = "sequence";
        public static final String CUSTOM_MAX_LEVEL = "custom_max_level";

        private SequenceConfig() {
            name = "[unregistered]";
            sequence = 0;
            customMaxLevel = 0;
        }

        public int getSequence() {
            return sequence;
        }

        public boolean hasCustomMaxLevel() {
            return customMaxLevel > 0;
        }

        public int getCustomMaxLevel() {
            return -1;
        }
    }
}
