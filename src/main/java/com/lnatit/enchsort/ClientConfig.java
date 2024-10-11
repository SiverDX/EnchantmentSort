package com.lnatit.enchsort;

import com.mojang.serialization.DataResult;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.lnatit.enchsort.EnchSort.LOGGER;
import static net.minecraft.ChatFormatting.getByName;

public class ClientConfig {
    public static ModConfigSpec SPEC;

    public static ModConfigSpec.BooleanValue SORT_BY_LEVEL;
    public static ModConfigSpec.BooleanValue SORT_BOOKS;
    public static ModConfigSpec.BooleanValue ASCENDING_SORT;
    public static ModConfigSpec.BooleanValue SNEAK_DISPLAY;
    public static ModConfigSpec.BooleanValue COMPATIBLE_MODE;

    public static ModConfigSpec.BooleanValue INDEPENDENT_TREASURE;
    public static ModConfigSpec.BooleanValue REVERSE_TREASURE;
    public static ModConfigSpec.BooleanValue HIGHLIGHT_TREASURE;
    public static ModConfigSpec.ConfigValue<List<? extends String>> TREASURE_FORMAT;

    public static ModConfigSpec.BooleanValue SHOW_MAX_LEVEL;
    public static ModConfigSpec.ConfigValue<List<? extends String>> MAX_LEVEL_FORMAT;

    public static final List<String> DEFAULT_FORMAT = new ArrayList<>();
    public static Style MAX_LEVEL, TREASURE;

    public static ModConfigSpec.BooleanValue HANDLE_DESCRIPTION;

    static {
        DEFAULT_FORMAT.add("DARK_GRAY");
        MAX_LEVEL = Style.EMPTY;
        TREASURE = Style.EMPTY;

        ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        SORT_BY_LEVEL = BUILDER
                .comment(" Sort the enchantments by its level",
                        " default: true")
                .define("sort_by_level", true);

        SORT_BOOKS = BUILDER
                .comment(" Sort enchantments on enchanted books",
                        " default: true")
                .define("sort_books", true);

        ASCENDING_SORT = BUILDER
                .comment(" Sort the enchantments in ascending order",
                        " default: false")
                .define("ascending_sort", false);

        SNEAK_DISPLAY = BUILDER
                .comment(" Display the original order when shift pressed",
                        " default: false")
                .define("sneak_display", false);

        COMPATIBLE_MODE = BUILDER
                .comment(" Compatible mode (may cause performance losses)",
                        " Enabling this config will disable [show_max_level] and [highlight_treasure]",
                        " default: false")
                .define("compatible_mode", false);

        HANDLE_DESCRIPTION = BUILDER
                .comment(" Properly sort the tooltip if enchantment descriptions are present",
                        " (Only needed if you notice issues)",
                        " default: false")
                .define("handle_description", false);

        BUILDER.push("Treasure");
        INDEPENDENT_TREASURE = BUILDER
                .comment(" Sort the treasure enchantments independently",
                        " default: true")
                .define("independent_treasure_sort", true);

        REVERSE_TREASURE = BUILDER
                .comment(" Sort the treasure enchantments in reverse",
                        " default: false")
                .define("reverse_treasure_sort", false);

        HIGHLIGHT_TREASURE = BUILDER
                .comment(" Enable custom highlights for treasure enchantments",
                        " default: false")
                .define("highlight_treasure", false);

        TREASURE_FORMAT = BUILDER
                .comment(" The format list of treasure enchantments",
                        " Use Formatting code or RGB format (#xxxxxx)")
                .defineList("treasure_format", DEFAULT_FORMAT, () -> "", ClientConfig::validateFormat);
        BUILDER.pop();

        BUILDER.push("MaxLevel");
        SHOW_MAX_LEVEL = BUILDER
                .comment(" Show the max level of the enchantments",
                        " default: false")
                .define("show_max_level", false);

        MAX_LEVEL_FORMAT = BUILDER
                .comment(" The format list of the max level text",
                        " Use Formatting code or RGB format (#xxxxxx)")
                .defineList("max_level_format", DEFAULT_FORMAT, () -> "", ClientConfig::validateFormat);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    private static boolean validateFormat(final Object entry) {
        if (entry instanceof String string) {
            // the named colors are stored (and therefor checked) in lowercase
            string = string.toLowerCase(Locale.ENGLISH);
            DataResult<TextColor> result = TextColor.parseColor(string);
            return result.isSuccess();
        }

        return false;
    }

    private static Style parseFormatList(final List<? extends String> formats) {
        Style style = Style.EMPTY;

        for (String element : formats) {
            element = element.toLowerCase(Locale.ENGLISH);

            ChatFormatting format;
            format = getByName(element);

            if (format != null) {
                style = style.applyFormat(format);
                continue;
            }

            DataResult<TextColor> result = TextColor.parseColor(element);

            if (result.isSuccess()) {
                style = style.withColor(result.getOrThrow());
            } else {
                LOGGER.warn("Format element {} parse failed, please check config file", element);
            }
        }

        return style;
    }

    public static void parseConfig(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != ClientConfig.SPEC) {
            return;
        }

        if (SHOW_MAX_LEVEL.get() || HIGHLIGHT_TREASURE.get()) {
            MAX_LEVEL = parseFormatList(MAX_LEVEL_FORMAT.get());
            TREASURE = parseFormatList(TREASURE_FORMAT.get());
            LOGGER.info("Special format parsed successfully");
        }
    }
}
