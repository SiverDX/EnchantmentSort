package com.lnatit.enchsort;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.ForgeConfigSpec;

import java.util.*;

import static com.lnatit.enchsort.EnchSort.LOGGER;
import static com.lnatit.enchsort.EnchSort.MOD_NAME;

public class EnchSortConfig
{
    public static ForgeConfigSpec CLIENT_CONFIG;
    public static ForgeConfigSpec.BooleanValue SORT_BY_LEVEL;
    public static ForgeConfigSpec.ConfigValue<List<String>> SORT_SEQUENCE;
    public static ForgeConfigSpec.BooleanValue INDEPENDENT_TREASURE;
    public static ForgeConfigSpec.BooleanValue REVERSE_TREASURE;
    public static ForgeConfigSpec.BooleanValue ALSO_SORT_BOOK;
    public static ForgeConfigSpec.BooleanValue ASCENDING_SORT;
    public static ForgeConfigSpec.BooleanValue SHOW_MAX_LEVEL;
    public static ForgeConfigSpec.ConfigValue<List<String>> MAX_LEVEL_FORMAT;
    public static ForgeConfigSpec.BooleanValue HIGHLIGHT_TREASURE;
    public static ForgeConfigSpec.ConfigValue<List<String>> TREASURE_FORMAT;

    public static final List<String> DEFAULT_SEQUENCE = new ArrayList<>();
    public static final List<String> DEFAULT_FORMAT = new ArrayList<>();
    public static final HashMap<String, Integer> ENCH_RANK = new HashMap<>();
    public static Style MAX_LEVEL, TREASURE;

    static
    {
        DEFAULT_SEQUENCE.add("minecraft:unbreaking");
        DEFAULT_SEQUENCE.add("minecraft:mending");

        DEFAULT_FORMAT.add("DARK_GRAY");
        MAX_LEVEL = Style.EMPTY;
        TREASURE = Style.EMPTY;

        ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

        BUILDER.comment(" Client Settings for " + MOD_NAME).push("client");

        // DONE
        SORT_BY_LEVEL = BUILDER
                .comment(" Whether sort the enchantments by its level",
                         " default: true"
                )
                .define("sotByLevel", true);

        // DONE
        SORT_SEQUENCE = BUILDER
                .comment(" Sequence of enchantments when sorting",
                         " modid:enchantment"
                )
                .define("sortSequence", DEFAULT_SEQUENCE);

        // DONE
        INDEPENDENT_TREASURE = BUILDER
                .comment(" Whether to sort the treasure enchantment independently",
                         " This configuration has further info, see [client.IndieTreasure]",
                         " default: true"
                )
                .define("indieTreasure", true);

        // DONE
        BUILDER.push("IndieTreasure");
        REVERSE_TREASURE = BUILDER
                .comment(" Whether to sort the treasure on reverse side",
                         " default: false"
                )
                .define("reverseTreasure", false);
        BUILDER.pop();

        // DONE
        ALSO_SORT_BOOK = BUILDER
                .comment(" Whether sort the enchantments on enchanted book",
                         " default: false"
                )
                .define("alsoSortBook", false);

        // DONE
        ASCENDING_SORT = BUILDER
                .comment(" Sort the enchantments in ascending order",
                         " default: false"
                )
                .define("ascendingSort", false);

        // DONE
        SHOW_MAX_LEVEL = BUILDER
                .comment(" Whether to show the max level of the enchantments",
                         " This configuration has further info, see [client.ShowMaxLevel]",
                         " default: false"
                )
                .define("showMaxLevel", false);

        // DONE
        BUILDER.push("ShowMaxLevel");
        MAX_LEVEL_FORMAT = BUILDER
                .comment(" The format list of the max level text",
                         " Use Formatting code or RGB format (#xxxxxx)"
                )
                .define("maxLevelFormat", DEFAULT_FORMAT);
        BUILDER.pop();

        // DONE
        HIGHLIGHT_TREASURE = BUILDER
                .comment(" Whether to highlight the treasure enchantments (except curse)",
                         " This configuration has further info, see [client.HighLightTreasure]",
                         " default: false"
                )
                .define("highlightTreasure", false);

        // DONE
        BUILDER.push("HighlightTreasure");
        TREASURE_FORMAT = BUILDER
                .comment(" The format list of treasure enchantments",
                         " Use Formatting code or RGB format (#xxxxxx)"
                )
                .define("treasureFormat", DEFAULT_FORMAT);
        BUILDER.pop();

        BUILDER.pop();

        CLIENT_CONFIG = BUILDER.build();
    }

    private static Style parseFormatList(List<String> formats)
    {
        Style style = Style.EMPTY;
        for (String fElement : formats)
        {
            fElement = fElement.toUpperCase();

            ChatFormatting format = ChatFormatting.getByName(fElement);
            if (format != null)
            {
                style = style.applyFormat(format);
                continue;
            }

            TextColor color = TextColor.parseColor(fElement);
            if (color != null)
            {
                style = style.withColor(color);
                continue;
            }

            LOGGER.warn("Format element " + fElement + " parse failed, please check config file.");
        }

        return style;
    }

    public static void parseConfig()
    {
        int size, index;
        List<String> sequence = SORT_SEQUENCE.get();
        size = sequence.size();

        for (index = 0; index < size; index++)
            ENCH_RANK.put(sequence.get(index), size - index);

        if (ENCH_RANK.size() == index)
            LOGGER.info("Parsed " + index + " enchantments successful!");
        else LOGGER.warn("Parse count dismatch!!! There are " + (index - ENCH_RANK.size()) + " repeats.");

        if (SHOW_MAX_LEVEL.get() || HIGHLIGHT_TREASURE.get())
        {
            MAX_LEVEL = parseFormatList(MAX_LEVEL_FORMAT.get());
            TREASURE = parseFormatList(TREASURE_FORMAT.get());
            LOGGER.info("Special format parsed successful!");
        }
    }

    public static Component getFullEnchLine(Map.Entry<Enchantment, Integer> entry)
    {
        Enchantment enchantment = entry.getKey();
        int level = entry.getValue();
        MutableComponent mutablecomponent = Component.translatable(enchantment.getDescriptionId());

        if (enchantment.isCurse())
            mutablecomponent.withStyle(ChatFormatting.RED);
        else if (HIGHLIGHT_TREASURE.get() && enchantment.isTreasureOnly())
            mutablecomponent.withStyle(TREASURE);
        else
            mutablecomponent.withStyle(ChatFormatting.GRAY);

        if (level != 1 || enchantment.getMaxLevel() != 1)
        {
            mutablecomponent.append(" ").append(Component.translatable("enchantment.level." + level));
            if (SHOW_MAX_LEVEL.get())
            {
                Component maxLvl = Component
                        .literal("/")
                        .append(Component.translatable("enchantment.level." + enchantment.getMaxLevel()))
                        .setStyle(MAX_LEVEL);
                mutablecomponent.append(maxLvl);
            }
        }

        return mutablecomponent;
    }

    static class EnchComparator implements Comparator<Map.Entry<Enchantment, Integer>>
    {
        private static int maxEnchLvl;
        private static int enchCount;
        private static final EnchComparator instance = new EnchComparator();

        private EnchComparator()
        {
        }

        public static Comparator<Map.Entry<Enchantment, Integer>> getInstance()
        {
            if (ASCENDING_SORT.get())
                return instance;
            else return instance.reversed();
        }

        public static void InitComparator()
        {
            enchCount = Registry.ENCHANTMENT.size();
            if (enchCount == 0)
                LOGGER.warn("Enchantments...  Where are the enchantments???!");

            maxEnchLvl = 1;
            for (Enchantment ench : Registry.ENCHANTMENT)
                if (ench.getMaxLevel() > maxEnchLvl)
                    maxEnchLvl = ench.getMaxLevel();
            LOGGER.info("Max enchantment level is " + maxEnchLvl + ".");
        }

        @Override
        public int compare(Map.Entry<Enchantment, Integer> o1, Map.Entry<Enchantment, Integer> o2)
        {
            int r1 = 0, r2 = 0, ret;
            ResourceLocation e1 = EnchantmentHelper.getEnchantmentId(o1.getKey());
            ResourceLocation e2 = EnchantmentHelper.getEnchantmentId(o2.getKey());

            if (e1 == null)
                LOGGER.error("Failed to get enchantment: " + o1.getKey().getDescriptionId() + "!!!");
            else r1 = ENCH_RANK.getOrDefault(e1.toString(), 0);
            if (e2 == null)
                LOGGER.error("Failed to get enchantment: " + o2.getKey().getDescriptionId() + "!!!");
            else r2 = ENCH_RANK.getOrDefault(e2.toString(), 0);

            ret = r1 - r2;

            if (SORT_BY_LEVEL.get())
                ret += (o1.getValue() - o2.getValue()) * enchCount;

            if (INDEPENDENT_TREASURE.get())
            {
                int treasureModify = maxEnchLvl * enchCount;
                if (REVERSE_TREASURE.get())
                    treasureModify = -treasureModify;

                if (o1.getKey().isTreasureOnly())
                    ret -= treasureModify;
                if (o2.getKey().isTreasureOnly())
                    ret += treasureModify;
            }

            return ret;
        }
    }
}
