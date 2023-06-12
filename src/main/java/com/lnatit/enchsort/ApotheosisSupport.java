package com.lnatit.enchsort;

import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

import static com.lnatit.enchsort.EnchSort.LOGGER;

//Thanks for SiverDX's help
public class ApotheosisSupport
{
    private static final MethodHandle getMaxLevel;
    private static final MethodHandle isTreasureOnly;

    static
    {
        Method maxLevelMethod;
        Method treasureOnlyMethod;

        try
        {
            Class<?> EnchHooks = Class.forName("shadows.apotheosis.ench.asm.EnchHooks");
            maxLevelMethod = EnchHooks.getMethod("getMaxLevel", Enchantment.class);
            treasureOnlyMethod = EnchHooks.getMethod("isTreasureOnly", Enchantment.class);
        }
        catch (Throwable exception)
        {
            LOGGER.debug("Apotheosis Support not loaded.\n" + exception);

            maxLevelMethod = ObfuscationReflectionHelper.findMethod(Enchantment.class, "m_6586_");
            treasureOnlyMethod = ObfuscationReflectionHelper.findMethod(Enchantment.class, "m_6591_");
        }

        try
        {
            maxLevelMethod.setAccessible(true);
            treasureOnlyMethod.setAccessible(true);
            getMaxLevel = MethodHandles.lookup().unreflect(maxLevelMethod);
            isTreasureOnly = MethodHandles.lookup().unreflect(treasureOnlyMethod);
        }
        catch (IllegalAccessException exception)
        {
            LOGGER.warn("Failed to load Apotheosis Support!");
            throw new RuntimeException("Failed to access Enchantment#getMaxLevel / Enchantment#isTreasureOnly!");
        }
    }

    public static boolean isTreasure(final Enchantment enchantment)
    {
        boolean isTreasure;

        try
        {
            isTreasure = (Boolean) isTreasureOnly.invoke(enchantment);
        }
        catch (Throwable e)
        {
            isTreasure = enchantment.isTreasureOnly();
        }

        return isTreasure;
    }

    public static int getMaxLevel(final Enchantment enchantment)
    {
        int maxLevel;

        try
        {
            maxLevel = (Integer) getMaxLevel.invoke(enchantment);
        }
        catch (Throwable e)
        {
            maxLevel = enchantment.getMaxLevel();
        }

        return maxLevel;
    }
}
