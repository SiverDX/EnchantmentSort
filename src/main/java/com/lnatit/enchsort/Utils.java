package com.lnatit.enchsort;

import dev.shadowsoffire.apothic_enchanting.asm.EnchHooks;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.List;

public class Utils {
    public static int getMaxLevel(final Holder<Enchantment> enchantment) {
        TomlHandler.SequenceConfig config = TomlHandler.getConfig(enchantment.getRegisteredName());

        if (config.hasCustomMaxLevel()) {
            return config.getCustomMaxLevel();
        }

        if (EnchSort.APOTH_ENCHANTING) {
            try {
                return EnchHooks.getMaxLevel(enchantment.value());
            } catch (UnsupportedOperationException ignored) {
                // Can't do much here, logging the exception would just spam things
            }
        }

        return enchantment.value().getMaxLevel();
    }

    public static boolean isTreasure(final Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.TREASURE);
    }

    public static boolean isCurse(final Holder<Enchantment> enchantment) {
        return enchantment.is(EnchantmentTags.CURSE);
    }

    public static List<Object2IntMap.Entry<Holder<Enchantment>>> getSortedEnchantments(final ItemStack stack, final HolderLookup.RegistryLookup<Enchantment> lookup) {
        ItemEnchantments enchantments;

        if (stack.getItem() instanceof EnchantedBookItem) {
            enchantments = stack.get(DataComponents.STORED_ENCHANTMENTS);
        } else {
            enchantments = stack.getAllEnchantments(lookup);
        }

        if (enchantments == null) {
            return List.of();
        }

        return enchantments.entrySet().stream().sorted(EnchSortRule.EnchComparator.getInstance()).toList();
    }
}
