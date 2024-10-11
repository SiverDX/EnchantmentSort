package com.lnatit.enchsort;

import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
import org.slf4j.Logger;

import static com.lnatit.enchsort.ClientConfig.COMPATIBLE_MODE;
import static com.lnatit.enchsort.ClientConfig.SNEAK_DISPLAY;

@Mod(value = EnchSort.MOD_ID, dist = Dist.CLIENT)
public class EnchSort {
    public static final String MOD_ID = "enchsort";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final boolean APOTH_ENCHANTING = ModList.get().isLoaded("apothic_enchanting");
    public static final boolean ENCHANTMENT_DESCRIPTIONS = ModList.get().isLoaded("enchdesc");

    public EnchSort(final IEventBus bus, final ModContainer container) {
        bus.addListener(EventPriority.LOW, ClientConfig::parseConfig);

        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, EnchSort::sortEnchantments);
        NeoForge.EVENT_BUS.addListener(EnchSortRule::initializeRules);

        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
    }

    private static void sortEnchantments(final ItemTooltipEvent event) {
        ItemStack stack = event.getItemStack();

        if (event.getEntity() == null) {
            return;
        }

        if (SNEAK_DISPLAY.get() && Screen.hasShiftDown()) {
            return;
        }

        boolean isEnchantedBook = stack.getItem() instanceof EnchantedBookItem;

        if (!ClientConfig.SORT_BOOKS.get() && isEnchantedBook || !isEnchantedBook && !stack.isEnchanted()) {
            return;
        }

        if (event.getContext().registries() == null) {
            return;
        }

        if (COMPATIBLE_MODE.get()) {
            EnchSortRule.sortCompatible(event);
        } else {
            EnchSortRule.sortDefault(event);
        }
    }
}
