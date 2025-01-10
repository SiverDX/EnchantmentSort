package com.lnatit.enchsort;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class EnchSortRule {
    public static void sortDefault(final ItemTooltipEvent event) {
        //noinspection DataFlowIssue -> registry shoudl be present
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
            EnchSort.LOGGER.warn("Some tooltips are missing - try using the [compatible] mode");
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

                if (description != null) {
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

    private static @Nullable String getDescriptionKey(final Component component) {
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

    public static Component getEnchantmentTooltip(final Object2IntMap.Entry<Holder<Enchantment>> entry) {
        Component description = Enchantment.getFullname(entry.getKey(), entry.getIntValue());

        if (!(description instanceof MutableComponent mutable)) {
            return description;
        }

        int level = entry.getIntValue();
        int maxLevel = Utils.getMaxLevel(entry.getKey());

        if (ClientConfig.CURSE_STYLE != Style.EMPTY && Utils.isCurse(entry.getKey())) {
            mutable.withStyle(ClientConfig.CURSE_STYLE);
        } else if (ClientConfig.TREASURE_STYLE != Style.EMPTY && Utils.isTreasure(entry.getKey())) {
            mutable.withStyle(ClientConfig.TREASURE_STYLE);
        } else if (ClientConfig.MAX_LEVEL_STYLE != Style.EMPTY && level == maxLevel) {
            mutable.withStyle(ClientConfig.MAX_LEVEL_STYLE);
        }

        if (level != 1 || maxLevel != 1) {
            if (ClientConfig.SHOW_MAX_LEVEL.get()) {
                Component maxLvl = Component
                        .literal("/")
                        .append(Component.translatable("enchantment.level." + maxLevel))
                        .setStyle(ClientConfig.MAX_LEVEL_INFO_STYLE);

                mutable.append(maxLvl);
            }
        }

        return mutable;
    }

    static class EnchComparator implements Comparator<Object2IntMap.Entry<Holder<Enchantment>>> {
        private static final EnchComparator INSTANCE = new EnchComparator();

        public static Comparator<Object2IntMap.Entry<Holder<Enchantment>>> getInstance() {
            if (ClientConfig.ASCENDING_SORT.get()) {
                return INSTANCE;
            }

            return INSTANCE.reversed();
        }

        @Override
        public int compare(final Object2IntMap.Entry<Holder<Enchantment>> first, final Object2IntMap.Entry<Holder<Enchantment>> second) {
            TomlHandler.SequenceConfig firstProperty = TomlHandler.getConfig(first.getKey().getRegisteredName());
            TomlHandler.SequenceConfig secondProperty = TomlHandler.getConfig(second.getKey().getRegisteredName());

            /*
            1. Sort by custom user sequence
            2. If the sequence is the same, sort using the levels (if the config is enabled)
            3. Sort treasure and / or curse enchantment separately (depending on the configs)
            */

            int result = Integer.compare(firstProperty.getSequence(), secondProperty.getSequence());

            if (result == 0 && ClientConfig.SORT_BY_LEVEL.get()) {
                result = Integer.compare(first.getIntValue(), second.getIntValue());

                if (result == 0) {
                    result = Integer.compare(Utils.getMaxLevel(first.getKey()), Utils.getMaxLevel(second.getKey()));
                }
            }

            // Curse enchantments are usually also tagged as treasure, therefor check them first
            if (ClientConfig.INDEPENDENT_CURSE.get()) {
                boolean isFirstCurse = Utils.isCurse(first.getKey());
                boolean isSecondCurse = Utils.isCurse(second.getKey());

                // Reverse = they start at the top
                if (isFirstCurse && !isSecondCurse) {
                    return ClientConfig.REVERSE_CURSE.get() ? -1 : 1;
                }

                if (!isFirstCurse && isSecondCurse) {
                    return ClientConfig.REVERSE_CURSE.get() ? 1 : -1;
                }
            }

            if (ClientConfig.INDEPENDENT_TREASURE.get()) {
                boolean isFirstTreasure = Utils.isTreasure(first.getKey());
                boolean isSecondTreasure = Utils.isTreasure(second.getKey());

                // Reverse = they start at the top
                if (isFirstTreasure && !isSecondTreasure) {
                    return ClientConfig.REVERSE_TREASURE.get() ? 1 : -1;
                }

                if (!isFirstTreasure && isSecondTreasure) {
                    return ClientConfig.REVERSE_TREASURE.get() ? 1 : -1;
                }
            }

            return result;
        }
    }
}
