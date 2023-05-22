package com.lnatit.enchsort;

import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;

import static com.lnatit.enchsort.EnchSort.LOGGER;
import static com.lnatit.enchsort.EnchSort.MOD_ID;
import static net.minecraft.ChatFormatting.GRAY;
import static net.minecraft.ChatFormatting.RED;

public class EnchSortRule
{
    private static File RULE_FILE;
    private static Toml RULE_TOML;

    private static final String FILE_NAME = MOD_ID + "-rule.toml";
    private static final String LIST_KEY = "entries";
    private static final EnchProperty DEFAULT_PROP = new EnchProperty();
    private static final HashMap<String, EnchProperty> ENCH_RANK = new HashMap<>();

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
            EnchSort.LOGGER.debug("Apotheosis Support not loaded.\n" + exception);

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
            throw new RuntimeException("Failed to access Enchantment#getMaxLevel / Enchantment#isTreasureOnly!");
        }
    }

    public static void initRule()
    {
        RULE_FILE = FMLPaths.CONFIGDIR.get().resolve(FILE_NAME).toFile();

        try
        {
            try
            {
                if (!RULE_FILE.exists() || RULE_FILE.createNewFile())
                    writeDefault();

                RULE_TOML = new Toml().read(RULE_FILE);
            }
            catch (RuntimeException e)
            {
                LOGGER.warn(FILE_NAME + " contains invalid toml, try regenerating...");
                writeDefault();
            }
        }
        catch (IOException e)
        {
            LOGGER.error("Failed to create or write " + FILE_NAME + "!!!");
            LOGGER.error(Arrays.toString(e.getStackTrace()));
        }

        parseRule();
        EnchComparator.initComparator();
    }

    public static void parseRule()
    {
        int size, index;
        List<Toml> tomlList = RULE_TOML.getTables(LIST_KEY);
        size = tomlList.size();

        for (index = 0; index < size; index++)
        {
            Toml entry = tomlList.get(index);
            EnchProperty prop = entry.to(EnchProperty.class);
            prop.sequence = size - index;
            ENCH_RANK.put(prop.name, prop);
        }

        if (ENCH_RANK.size() == index)
            LOGGER.info("Parsed " + index + " enchantments successful!");
        else LOGGER.warn("Parse count mismatch!!! There are " + (index - ENCH_RANK.size()) + " repeats.");
    }

    public static void sortDefault(List<Component> toolTip, ItemStack stack)
    {
        int index;
        // Since it's hard to sort Component directly, sort the enchMap instead
        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
        Set<Enchantment> enchs = enchMap.keySet();

        // find index & clear Enchantment Components
        for (index = 1; index < toolTip.size(); index++)
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
            LOGGER.warn("Some tooltip lines are missing, please try to enable {compatibleMode} in config!");
            return;
        }
        toolTip.subList(index, index + enchs.size()).clear();

        // Sort the enchMap & generate toolTip
        ArrayList<Map.Entry<Enchantment, Integer>> enchArray = new ArrayList<>(enchMap.entrySet());

        enchArray.sort(EnchSortRule.EnchComparator.getInstance());
        for (Map.Entry<Enchantment, Integer> entry : enchArray)
            toolTip.add(index++, getFullEnchLine(entry));
    }

    public static void sortCompatible(List<Component> toolTip, ItemStack stack)
    {
        int index;

        // Sort the enchMap first
        Map<Enchantment, Integer> enchMap = EnchantmentHelper.getEnchantments(stack);
        Set<Map.Entry<Enchantment, Integer>> enchs = enchMap.entrySet();
        ArrayList<Map.Entry<Enchantment, Integer>> enchArray = new ArrayList<>(enchs);

        enchArray.sort(EnchSortRule.EnchComparator.getInstance());

        // Get the according line index & component
        // In case some misc info was added before, store the upcoming one line if needed
        ArrayList<Integer> lineIndex = new ArrayList<>();
        Map<Map.Entry<Enchantment, Integer>, Component> lineComponent = new LinkedHashMap<>();

        for (index = 1; index < toolTip.size(); index++)
        {
            Component line = toolTip.get(index);

            if (line.getContents() instanceof TranslatableContents contents)
            {
                for (Map.Entry<Enchantment, Integer> ench : enchs)
                {
                    if (contents.getKey().equals(ench.getKey().getDescriptionId()))
                    {
                        lineIndex.add(index);
                        lineComponent.put(ench, line);
                    }
                }
            }
            else
            {
                for (Component elem : line.getSiblings())
                {
                    if (elem.getContents() instanceof TranslatableContents scontents)
                    {
                        for (Map.Entry<Enchantment, Integer> sench : enchs)
                        {
                            if (scontents.getKey().equals(sench.getKey().getDescriptionId()))
                            {
                                lineIndex.add(index);
                                lineComponent.put(sench, line);
                            }
                        }
                    }
                }
            }
        }

        // Insert tooltips back
        for (index = 0; index < lineIndex.size(); index++)
            toolTip.set(lineIndex.get(index), lineComponent.get(enchArray.get(index)));
    }

    private static void writeDefault() throws IOException
    {
        TomlWriter writer = new TomlWriter.Builder()
                .indentValuesBy(0)
                .indentTablesBy(0)
                .build();

        List<Map<String, Object>> entry = new ArrayList<>();

        for (Enchantment ench : ForgeRegistries.ENCHANTMENTS)
        {
            Map<String, Object> elem = new LinkedHashMap<>();
            ResourceLocation rl = EnchantmentHelper.getEnchantmentId(ench);
            if (rl == null)
            {
                LOGGER.error("Failed to get enchantment: " + ench.getDescriptionId() + "!!!");
                continue;
            }
            elem.put(EnchProperty.NAME, rl.toString());
            elem.put(EnchProperty.MAX_LVL, getMaxLevel(ench));

            entry.add(elem);
        }

        Map<String, List<Map<String, Object>>> default_sequence = new HashMap<>();
        default_sequence.put(LIST_KEY, entry);

        writer.write(default_sequence, RULE_FILE);
    }

    public static EnchProperty getPropById(String id)
    {
        return ENCH_RANK.getOrDefault(id, DEFAULT_PROP);
    }

    public static Component getFullEnchLine(Map.Entry<Enchantment, Integer> entry)
    {
        Enchantment enchantment = entry.getKey();
        int level = entry.getValue();
        MutableComponent mutablecomponent = Component.translatable(enchantment.getDescriptionId());

        if (enchantment.isCurse())
            mutablecomponent.withStyle(RED);
        else if (EnchSortConfig.HIGHLIGHT_TREASURE.get() && isTreasure(enchantment))
            mutablecomponent.withStyle(EnchSortConfig.TREASURE);
        else
            mutablecomponent.withStyle(GRAY);

        ResourceLocation rl = EnchantmentHelper.getEnchantmentId(enchantment);
        if (rl == null)
        {
            LOGGER.error("Failed to get enchantment: " + enchantment.getDescriptionId() + "!!!");
            return mutablecomponent;
        }
        int maxLevel = Math.max(getMaxLevel(enchantment), getPropById(rl.toString()).getMaxLevel());

        if (level != 1 || maxLevel != 1)
        {
            mutablecomponent.append(" ").append(Component.translatable("enchantment.level." + level));
            if (EnchSortConfig.SHOW_MAX_LEVEL.get())
            {
                Component maxLvl = Component
                        .literal("/")
                        .append(Component.translatable("enchantment.level." + maxLevel))
                        .setStyle(EnchSortConfig.MAX_LEVEL);
                mutablecomponent.append(maxLvl);
            }
        }

        return mutablecomponent;
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

    protected static class EnchProperty
    {
        private final String name;
        private int sequence;
        private final int max_lvl;

        public static final String NAME = "name";
        public static final String MAX_LVL = "max_lvl";

        private EnchProperty()
        {
            name = "null";
            sequence = 0;
            max_lvl = 0;
        }

        public int getSequence()
        {
            return sequence;
        }

        public int getMaxLevel()
        {
            return max_lvl;
        }
    }

    static class EnchComparator implements Comparator<Map.Entry<Enchantment, Integer>>
    {
        private static int maxEnchLvl;
        private static int enchCount;
        private static final EnchComparator INSTANCE = new EnchComparator();

        private EnchComparator()
        {
        }

        public static Comparator<Map.Entry<Enchantment, Integer>> getInstance()
        {
            if (EnchSortConfig.ASCENDING_SORT.get())
                return INSTANCE;
            else return INSTANCE.reversed();
        }

        public static void initComparator()
        {
            enchCount = ForgeRegistries.ENCHANTMENTS.getKeys().size();
            if (enchCount == 0)
                LOGGER.warn("Enchantments...  Where are the enchantments???!");

            maxEnchLvl = 1;
            for (Enchantment ench : ForgeRegistries.ENCHANTMENTS)
            {
                ResourceLocation rl = EnchantmentHelper.getEnchantmentId(ench);
                if (rl == null)
                {
                    LOGGER.error("Failed to get enchantment: " + ench.getDescriptionId() + "!!!");
                    continue;
                }
                int maxLevel = Math.max(getMaxLevel(ench), getPropById(rl.toString()).getMaxLevel());
                if (maxLevel > maxEnchLvl)
                    maxEnchLvl = maxLevel;
            }
            LOGGER.debug("Max enchantment level is " + maxEnchLvl + ".");
        }

        @Override
        public int compare(Map.Entry<Enchantment, Integer> o1, Map.Entry<Enchantment, Integer> o2)
        {
            int r1 = 0, r2 = 0, ret;
            ResourceLocation e1 = EnchantmentHelper.getEnchantmentId(o1.getKey());
            ResourceLocation e2 = EnchantmentHelper.getEnchantmentId(o2.getKey());

            if (e1 == null)
                LOGGER.error("Failed to get enchantment: " + o1.getKey().getDescriptionId() + "!!!");
            else r1 = getPropById(e1.toString()).getSequence();
            if (e2 == null)
                LOGGER.error("Failed to get enchantment: " + o2.getKey().getDescriptionId() + "!!!");
            else r2 = getPropById(e2.toString()).getSequence();

            ret = r1 - r2;

            if (EnchSortConfig.SORT_BY_LEVEL.get())
                ret += (o1.getValue() - o2.getValue()) * enchCount;

            if (EnchSortConfig.INDEPENDENT_TREASURE.get())
            {
                int treasureModify = maxEnchLvl * enchCount;
                if (EnchSortConfig.REVERSE_TREASURE.get())
                    treasureModify = -treasureModify;

                if (isTreasure(o1.getKey()))
                    ret -= treasureModify;
                if (isTreasure(o2.getKey()))
                    ret += treasureModify;
            }

            return ret;
        }
    }
}
