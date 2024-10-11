package com.lnatit.enchsort;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.lnatit.enchsort.EnchSort.LOGGER;
import static com.lnatit.enchsort.EnchSort.MOD_ID;
import static net.minecraft.ChatFormatting.GRAY;
import static net.minecraft.ChatFormatting.RED;

public class EnchSortRule {
    private static File RULE_FILE;
    private static Toml RULE_TOML;

    private static final String FILE_NAME = MOD_ID + "-rule.toml";
    private static final String LIST_KEY = "entries";
    private static final EnchProperty DEFAULT_PROP = new EnchProperty();
    private static final HashMap<String, EnchProperty> ENCH_RANK = new HashMap<>();

    // Enchantments are datapack registries, i.e. they're only fully present in a world
    public static void initializeRules(final EntityJoinLevelEvent event) {
        if (event.getEntity() != Minecraft.getInstance().player) {
            return;
        }

        Registry<Enchantment> registry = event.getEntity().level().registryAccess().registryOrThrow(Registries.ENCHANTMENT);
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
        EnchComparator.initComparator(registry);
    }

    public static void parseRule() {
        int size, index;
        List<Toml> tomlList = RULE_TOML.getTables(LIST_KEY);
        size = tomlList == null ? 0 : tomlList.size();

        for (index = 0; index < size; index++) {
            Toml entry = tomlList.get(index);
            EnchProperty prop = entry.to(EnchProperty.class);
            prop.sequence = size - index;
            ENCH_RANK.put(prop.name, prop);
        }

        if (ENCH_RANK.size() == index) {
            LOGGER.info("Parsed {} enchantments successfully", index);
        } else {
            LOGGER.warn("Parse count mismatch - there are {} repeats", index - ENCH_RANK.size());
        }
    }

    @SuppressWarnings("DataFlowIssue") // enchantment registry should be present
    public static void sortDefault(final ItemTooltipEvent event) {
        List<Object2IntMap.Entry<Holder<Enchantment>>> sorted = Utils.getSortedEnchantments(event.getItemStack(), event.getContext().registries().lookupOrThrow(Registries.ENCHANTMENT));

        int index;
        List<Component> tooltips = event.getToolTip();

        // Find index of the start of the enchantment tooltip
        for (index = 1; index < tooltips.size(); index++) {
            Component line = tooltips.get(index);

            if (line.getContents() instanceof TranslatableContents contents) {
                boolean foundEnchantment = false;

                for (Object2IntMap.Entry<Holder<Enchantment>> enchantment : sorted) {
                    if (contents.equals(enchantment.getKey().value().description().getContents())) {
                        foundEnchantment = true;
                        break;
                    }
                }

                if (foundEnchantment) {
                    break;
                }
            }
        }

        boolean handleDescriptions = EnchSort.ENCHANTMENT_DESCRIPTIONS || ClientConfig.HANDLE_DESCRIPTION.get();

        // Remove existing enchantment tooltips
        int toRemove = index + (handleDescriptions ? sorted.size() * 2 : sorted.size());

        if (toRemove > tooltips.size()) {
            LOGGER.warn("Some tooltips are missing - try using the [compatible] mode");
            return;
        }

        Map<String, Component> descriptions = new HashMap<>();
        List<Component> enchantmentTooltips = tooltips.subList(index, toRemove);

        if (handleDescriptions) {
            for (int i = 0; i < enchantmentTooltips.size(); i++) {
                Component component = enchantmentTooltips.get(i);

                if (isDescription(component)) {
                    if (enchantmentTooltips.get(i - 1).getContents() instanceof TranslatableContents enchantment) {
                        descriptions.put(enchantment.getKey(), component);
                    }
                }
            }
        }

        enchantmentTooltips.clear();

        // Add the sorted tooltips
        for (Object2IntMap.Entry<Holder<Enchantment>> entry : sorted) {
            Component component = getEnchantmentTooltip(entry);
            tooltips.add(index++, component);

            if (!descriptions.isEmpty() && component.getContents() instanceof TranslatableContents contents) {
                Component description = descriptions.remove(contents.getKey());

                if (descriptions != null) {
                    tooltips.add(index++, description);
                }
            }
        }
    }

    @SuppressWarnings("DataFlowIssue") // enchantment registry should be present
    public static void sortCompatible(final ItemTooltipEvent event) {
        List<Object2IntMap.Entry<Holder<Enchantment>>> sorted = Utils.getSortedEnchantments(event.getItemStack(), event.getContext().registries().lookupOrThrow(Registries.ENCHANTMENT));

        // Stores the current enchantment indexes and components
        ArrayList<Integer> indexes = new ArrayList<>();
        Map<String, Component> descriptions = new HashMap<>();
        Map<Object2IntMap.Entry<Holder<Enchantment>>, Component> components = new LinkedHashMap<>();

        int index;
        List<Component> tooltips = event.getToolTip();

        for (index = 1; index < tooltips.size(); index++) {
            Component line = tooltips.get(index);

            if (line.getContents() instanceof TranslatableContents contents) {
                for (Object2IntMap.Entry<Holder<Enchantment>> enchantment : sorted) {
                    if (contents.equals(enchantment.getKey().value().description().getContents())) {
                        indexes.add(index);
                        components.put(enchantment, line);
                    }
                }
            } else {
                for (Component component : line.getSiblings()) {
                    if (component.getContents() instanceof TranslatableContents contents) {
                        String key = getDescriptionKey(component);

                        if (key != null) {
                            // Will be later retrieved by accessing it with the translation key for the enchantment
                            descriptions.put(key.substring(0, key.length() - ".desc".length()), line);
                        } else {
                            // Unsure: Mods might add some extra information?
                            for (Object2IntMap.Entry<Holder<Enchantment>> enchantment : sorted) {
                                if (contents.equals(enchantment.getKey().value().description().getContents())) {
                                    indexes.add(index);
                                    components.put(enchantment, line);
                                }
                            }
                        }
                    }
                }
            }
        }

        // Sort by replacing the tooltips
        for (index = 0; index < indexes.size(); index++) {
            Component enchantmentTooltip = components.get(sorted.get(index));
            tooltips.set(indexes.get(index), enchantmentTooltip);

            if (!descriptions.isEmpty() && enchantmentTooltip.getContents() instanceof TranslatableContents contents) {
                Component description = descriptions.get(contents.getKey());

                if (description != null) {
                    tooltips.set(indexes.get(index) + 1, description);
                }
            }
        }
    }

    private static boolean isDescription(final Component component) {
        return getDescriptionKey(component) != null;
    }

    private static String getDescriptionKey(final Component component) {
        if (component.getContents() instanceof TranslatableContents contents && contents.getKey().endsWith(".desc")) {
            return contents.getKey();
        }

        for (Component sibling : component.getSiblings()) {
            if (sibling.getContents() instanceof TranslatableContents contents && contents.getKey().endsWith(".desc")) {
                return contents.getKey();
            }
        }

        return null;
    }

    private static void writeDefault(final Registry<Enchantment> registry) throws IOException {
        TomlWriter writer = new TomlWriter.Builder()
                .indentValuesBy(0)
                .indentTablesBy(0)
                .build();

        List<Map<String, Object>> entries = new ArrayList<>();

        registry.entrySet().forEach(entry -> {
            Map<String, Object> element = new LinkedHashMap<>();
            element.put(EnchProperty.NAME, entry.getKey().location().toString());
            element.put(EnchProperty.MAX_LVL, Utils.getMaxLevel(entry.getValue()));

            entries.add(element);
        });

        Map<String, List<Map<String, Object>>> defaultSequence = new HashMap<>();
        defaultSequence.put(LIST_KEY, entries);

        writer.write(defaultSequence, RULE_FILE);
    }

    public static EnchProperty getPropById(String id) {
        return ENCH_RANK.getOrDefault(id, DEFAULT_PROP);
    }

    public static Component getEnchantmentTooltip(final Object2IntMap.Entry<Holder<Enchantment>> entry) {
        Enchantment enchantment = entry.getKey().value();
        Component description = enchantment.description();

        if (!(description instanceof MutableComponent mutable)) {
            return description;
        }

        // The description itself is immutable
        mutable = mutable.copy();

        if (Utils.isCurse(entry.getKey())) {
            mutable.withStyle(RED);
        } else if (ClientConfig.HIGHLIGHT_TREASURE.get() && Utils.isTreasure(entry.getKey())) {
            mutable.withStyle(ClientConfig.TREASURE);
        } else {
            mutable.withStyle(GRAY);
        }

        int level = entry.getIntValue();
        int maxLevel = Math.max(Utils.getMaxLevel(enchantment), getPropById(entry.getKey().getRegisteredName()).getMaxLevel());

        if (level != 1 || maxLevel != 1) {
            mutable.append(" ").append(Component.translatable("enchantment.level." + level));

            if (ClientConfig.SHOW_MAX_LEVEL.get()) {
                Component maxLvl = Component
                        .literal("/")
                        .append(Component.translatable("enchantment.level." + maxLevel))
                        .setStyle(ClientConfig.MAX_LEVEL);

                mutable.append(maxLvl);
            }
        }

        return mutable;
    }


    public static class EnchProperty {
        private final String name;
        private int sequence;
        private final int max_lvl;

        public static final String NAME = "name";
        public static final String MAX_LVL = "max_lvl";

        private EnchProperty() {
            name = "null";
            sequence = 0;
            max_lvl = 0;
        }

        public int getSequence() {
            return sequence;
        }

        public int getMaxLevel() {
            return max_lvl;
        }
    }

    static class EnchComparator implements Comparator<Object2IntMap.Entry<Holder<Enchantment>>> {
        private static int maxLevel;
        private static int enchantmentCount;
        private static final EnchComparator INSTANCE = new EnchComparator();

        private EnchComparator() {
        }

        public static Comparator<Object2IntMap.Entry<Holder<Enchantment>>> getInstance() {
            if (ClientConfig.ASCENDING_SORT.get()) {
                return INSTANCE;
            }

            return INSTANCE.reversed();
        }

        public static void initComparator(final Registry<Enchantment> registry) {
            enchantmentCount = registry.size();

            if (enchantmentCount == 0) {
                LOGGER.warn("There are no enchantments in the registry");
            }

            registry.entrySet().forEach(entry -> {
                int maxLevel = Math.max(Utils.getMaxLevel(entry.getValue()), getPropById(entry.getKey().location().toString()).getMaxLevel());

                if (maxLevel > EnchComparator.maxLevel) {
                    EnchComparator.maxLevel = maxLevel;
                }
            });

            LOGGER.debug("Max enchantment level is {}", EnchComparator.maxLevel);
        }

        @Override
        public int compare(final Object2IntMap.Entry<Holder<Enchantment>> first, final Object2IntMap.Entry<Holder<Enchantment>> second) {
            EnchProperty firstProperty = getPropById(first.getKey().getRegisteredName());
            EnchProperty secondProperty = getPropById(second.getKey().getRegisteredName());

            int result = firstProperty.getSequence() - secondProperty.getSequence();

            if (ClientConfig.SORT_BY_LEVEL.get()) {
                result += (first.getIntValue() - second.getIntValue()) * enchantmentCount;
            }

            if (ClientConfig.INDEPENDENT_TREASURE.get()) {
                // Ensures that treasure enchantments start at the highest or lowest position
                int modifier = maxLevel * enchantmentCount;

                if (ClientConfig.REVERSE_TREASURE.get()) {
                    modifier = -modifier;
                }

                if (Utils.isTreasure(first.getKey())) {
                    result -= modifier;
                }

                if (Utils.isTreasure(second.getKey())) {
                    result += modifier;
                }
            }

            return result;
        }
    }
}
