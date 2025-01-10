package com.lnatit.enchsort;

import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Style;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.Objects;
import java.util.function.Function;

public class ClientConfig {
    public static ModConfigSpec SPEC;

    public static ModConfigSpec.BooleanValue SORT_BY_LEVEL;
    public static ModConfigSpec.BooleanValue SORT_BOOKS;
    public static ModConfigSpec.BooleanValue ASCENDING_SORT;
    public static ModConfigSpec.BooleanValue SNEAK_DISPLAY;
    public static ModConfigSpec.BooleanValue COMPATIBLE_MODE;

    public static ModConfigSpec.BooleanValue INDEPENDENT_TREASURE;
    public static ModConfigSpec.BooleanValue REVERSE_TREASURE;
    public static ModConfigSpec.ConfigValue<String> TREASURE_FORMAT;

    public static ModConfigSpec.BooleanValue INDEPENDENT_CURSE;
    public static ModConfigSpec.BooleanValue REVERSE_CURSE;
    public static ModConfigSpec.ConfigValue<String> CURSE_FORMAT;

    public static ModConfigSpec.BooleanValue SHOW_MAX_LEVEL;
    public static ModConfigSpec.ConfigValue<String> MAX_LEVEL_FORMAT;
    public static ModConfigSpec.ConfigValue<String> MAX_LEVEL_INFO_FORMAT;

    public static Style MAX_LEVEL_STYLE = Style.EMPTY;
    public static Style MAX_LEVEL_INFO_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);
    public static Style TREASURE_STYLE = Style.EMPTY.withColor(ChatFormatting.GOLD);
    public static Style CURSE_STYLE = Style.EMPTY.withColor(ChatFormatting.DARK_RED);

    public static ModConfigSpec.BooleanValue HANDLE_DESCRIPTION;

    static {
        ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

        SORT_BY_LEVEL = BUILDER
                .comment(" Sort the enchantments by its level")
                .define("sort_by_level", true);

        SORT_BOOKS = BUILDER
                .comment(" Sort enchantments on enchanted books")
                .define("sort_books", true);

        ASCENDING_SORT = BUILDER
                .comment(" Sort the enchantments in ascending order")
                .define("ascending_sort", false);

        SNEAK_DISPLAY = BUILDER
                .comment(" Display the original order when shift pressed")
                .define("sneak_display", false);

        COMPATIBLE_MODE = BUILDER
                .comment(" Compatible mode (may cause performance losses)",
                        " Enabling this config will disable [show_max_level] and [highlight_treasure]")
                .define("compatible_mode", false);

        HANDLE_DESCRIPTION = BUILDER
                .comment(" Properly sort the tooltip if enchantment descriptions are present",
                        " (Only needed if you notice issues)")
                .define("handle_description", false);

        BUILDER.push("Treasure");
        INDEPENDENT_TREASURE = BUILDER
                .comment(" Sort the treasure enchantments independently")
                .define("independent_treasure_sort", true);

        REVERSE_TREASURE = BUILDER
                .comment(" Sort the treasure enchantments in reverse")
                .define("reverse_treasure_sort", false);

        TREASURE_FORMAT = BUILDER
                .comment(" The style to be used for treasure enchantments",
                        " Define the style data in the 'key:value,key:value' format")
                .define("treasure_style", encodeStyle(TREASURE_STYLE), entry -> entry instanceof String string && decodeStyle(string) != null);
        BUILDER.pop();

        BUILDER.push("Curse");
        INDEPENDENT_CURSE = BUILDER
                .comment(" Sort the curses independently")
                .define("independent_curse_sort", true);

        REVERSE_CURSE = BUILDER
                .comment(" Sort the curses in reverse")
                .define("reverse_curse_sort", false);

        CURSE_FORMAT = BUILDER
                .comment(" The style to be used for curses",
                        " Define the style data in the 'key:value,key:value' format")
                .define("curse_style", encodeStyle(CURSE_STYLE), entry -> entry instanceof String string && decodeStyle(string) != null);
        BUILDER.pop();

        BUILDER.push("MaxLevel");
        SHOW_MAX_LEVEL = BUILDER
                .comment(" Show the max level of the enchantments")
                .define("show_max_level", true);

        MAX_LEVEL_FORMAT = BUILDER
                .comment(" The style to be used for enchantments that reached max. level",
                        " Define the style data in the 'key:value,key:value' format")
                .define("max_level_style", encodeStyle(MAX_LEVEL_STYLE), entry -> entry instanceof String string && decodeStyle(string) != null);

        MAX_LEVEL_INFO_FORMAT = BUILDER
                .comment(" The style to be used for the max. level information part",
                        " Define the style data in the 'key:value,key:value' format")
                .define("max_level_info_style", encodeStyle(MAX_LEVEL_INFO_STYLE), entry -> entry instanceof String string && decodeStyle(string) != null);
        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    @SuppressWarnings("DuplicateBranchesInSwitch") // ignore for clarity
    public static String encodeStyle(final Style style) {
        if (style == Style.EMPTY) {
            return "";
        }

        CompoundTag tag = (CompoundTag) Style.Serializer.CODEC.encodeStart(NbtOps.INSTANCE, style).getOrThrow();
        StringBuilder builder = new StringBuilder();

        tag.getAllKeys().forEach(key -> {
            if (!builder.isEmpty()) {
                builder.append(",");
            }

            String entry = switch (key) {
                case "color" -> tag.getString(key);
                case "bold" -> String.valueOf(tag.getBoolean(key));
                case "italic" -> String.valueOf(tag.getBoolean(key));
                case "underlined" -> String.valueOf(tag.getBoolean(key));
                case "strikethrough" -> String.valueOf(tag.getBoolean(key));
                case "obfuscated" -> String.valueOf(tag.getBoolean(key));
                default -> "";
            };

            if (!entry.isEmpty()) {
                entry = key + ":" + entry;
                builder.append(entry);
            }
        });

        return builder.toString();
    }

    @SuppressWarnings("DuplicateBranchesInSwitch") // ignore for clarity
    public static Style decodeStyle(final String data) {
        if (data.isBlank()) {
            return Style.EMPTY;
        }

        CompoundTag tag = new CompoundTag();
        String[] split = data.split(",");

        for (String element : split) {
            String[] entry = element.split(":", 2);

            if (entry.length != 2) {
                return null;
            }

            String key = entry[0].trim();
            String value = entry[1].trim();

            switch (key) {
                case "color":
                    tag.putString(key, value);
                    break;
                case "bold":
                    tag.putBoolean(key, Boolean.parseBoolean(value));
                    break;
                case "italic":
                    tag.putBoolean(key, Boolean.parseBoolean(value));
                    break;
                case "underlined":
                    tag.putBoolean(key, Boolean.parseBoolean(value));
                    break;
                case "strikethrough":
                    tag.putBoolean(key, Boolean.parseBoolean(value));
                    break;
                case "obfuscated":
                    tag.putBoolean(key, Boolean.parseBoolean(value));
                    break;
                default:
                    return null;
            }
        }

        return Style.Serializer.CODEC.parse(NbtOps.INSTANCE, tag).mapOrElse(Function.identity(), error -> null);
    }

    public static void parseConfig(final ModConfigEvent event) {
        if (event.getConfig().getSpec() != ClientConfig.SPEC) {
            return;
        }

        MAX_LEVEL_STYLE = Objects.requireNonNullElse(decodeStyle(MAX_LEVEL_FORMAT.get()), MAX_LEVEL_STYLE);
        MAX_LEVEL_INFO_STYLE = Objects.requireNonNullElse(decodeStyle(MAX_LEVEL_INFO_FORMAT.get()), MAX_LEVEL_INFO_STYLE);
        TREASURE_STYLE = Objects.requireNonNullElse(decodeStyle(TREASURE_FORMAT.get()), TREASURE_STYLE);
        CURSE_STYLE = Objects.requireNonNullElse(decodeStyle(CURSE_FORMAT.get()), CURSE_STYLE);
    }
}
