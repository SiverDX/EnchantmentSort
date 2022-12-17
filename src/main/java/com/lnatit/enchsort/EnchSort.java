package com.lnatit.enchsort;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Registry;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.Environment;
import net.minecraftforge.registries.RegisterEvent;
import org.slf4j.Logger;

import java.util.*;

@Mod(EnchSort.MOD_ID)
public class EnchSort
{
    public static final String MOD_ID = "enchsort";
    public static final String MOD_NAME = "Enchantment Sort";

    public static final Logger LOGGER = LogUtils.getLogger();

    public EnchSort()
    {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, EnchSortConfig.CLIENT_CONFIG);

        if (Environment.get().getDist().isClient())
        {
            MinecraftForge.EVENT_BUS.addListener(EventPriority.HIGH, EnchSort::onItemDesc);
            FMLJavaModLoadingContext
                    .get()
                    .getModEventBus()
                    .addListener(EventPriority.LOW,
                                 (ModConfigEvent event) ->
                                 {
                                     EnchSortConfig.parseConfig();
                                     EnchSortConfig.EnchComparator.InitComparator();
                                 }
                    );
        }
    }


    private static void onItemDesc(ItemTooltipEvent event)
    {
        final ItemStack stack = event.getItemStack();

        boolean forSort = stack.isEnchanted() || (EnchSortConfig.ALSO_SORT_BOOK.get() && stack.getItem() instanceof EnchantedBookItem);
        forSort = forSort && (getHideFlags(stack) & ItemStack.TooltipPart.ENCHANTMENTS.getMask()) == 0;
        if (!forSort || event.getEntity() == null)
            return;

        int index;
        // Since it's hard to sort Component directly, sort the enchMap instead
        final List<Component> toolTip = event.getToolTip();
        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
        final Set<Enchantment> enchs = enchMap.keySet();

        // find index & clear Enchantment Components
        for (index = 0; index < toolTip.size(); index++)
        {
            Component line = toolTip.get(index);

            if (line.getContents() instanceof TranslatableContents contents)
            {
                boolean flag = false;

                for (Enchantment ench : enchs)
                    if (contents.getKey().equals(ench.getDescriptionId()))
                    {
                        flag = true;
                        break;
                    }

                if (flag)
                    break;
            }
        }
        if (index + enchs.size() > toolTip.size())
        {
            LOGGER.warn("Some tooltip lines are missing!!!");
            return;
        }
        toolTip.subList(index, index + enchs.size()).clear();

        // Sort the enchMap & generate toolTip
        ArrayList<Map.Entry<Enchantment, Integer>> enchArray = new ArrayList<>(enchMap.entrySet());

        enchArray.sort(EnchSortConfig.EnchComparator.getInstance());
        for (Map.Entry<Enchantment, Integer> entry : enchArray)
            toolTip.add(index++, EnchSortConfig.getFullEnchLine(entry));
    }

    private static int getHideFlags(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains("HideFlags", 99) ? stack.getTag().getInt("HideFlags") : stack.getItem().getDefaultTooltipHideFlags(stack);
    }
}
