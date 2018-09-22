package net.silentchaos512.gear.item;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.EnumRarity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipe;
import net.minecraft.item.crafting.Ingredient;
import net.minecraft.item.crafting.ShapelessRecipes;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.util.NonNullList;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.item.ICoreItem;
import net.silentchaos512.gear.api.item.ICoreTool;
import net.silentchaos512.gear.api.item.IStatItem;
import net.silentchaos512.gear.api.parts.ItemPartData;
import net.silentchaos512.gear.api.parts.PartDataList;
import net.silentchaos512.gear.api.parts.PartMain;
import net.silentchaos512.gear.api.parts.PartRegistry;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.StatInstance;
import net.silentchaos512.gear.client.util.GearClientHelper;
import net.silentchaos512.gear.client.util.TooltipFlagTC;
import net.silentchaos512.gear.init.ModItems;
import net.silentchaos512.gear.init.ModMaterials;
import net.silentchaos512.gear.item.blueprint.Blueprint;
import net.silentchaos512.gear.util.GearData;
import net.silentchaos512.lib.util.I18nHelper;
import net.silentchaos512.lib.util.StackHelper;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ToolHead extends Item implements IStatItem {
    private static final String NBT_ROOT = "ToolHeadData";
    private static final String NBT_TOOL_CLASS = "ToolClass";
    private static final String NBT_MATERIALS = "Materials";
    private static final String NBT_STAT_CACHE = "StatCache";
    private static final String NBT_IS_EXAMPLE = "IsExample";

    /**
     * Create a stack with the given materials. Best for getting crafting results.
     */
    public ItemStack getStack(String toolClass, Collection<ItemStack> materials) {
        return getStack(toolClass, PartDataList.from(materials));
    }

    public ItemStack getStack(String toolClass, PartDataList parts) {
        ItemStack result = new ItemStack(this);
        NBTTagCompound tags = getData(result);
        tags.setString(NBT_TOOL_CLASS, toolClass);

        NBTTagList tagList = new NBTTagList();
        parts.stream().filter(data -> data.getPart() instanceof PartMain)
                .map(data -> data.writeToNBT(new NBTTagCompound()))
                .forEach(tagList::appendTag);
        tags.setTag(NBT_MATERIALS, tagList);

        writeStatCache(result, parts);
        return result;
    }

    /**
     * Create a stack with a single part. Best for sub-items.
     */
    public ItemStack getStack(String toolClass, PartMain part, boolean isExample) {

        ItemStack result = new ItemStack(this);
        NBTTagCompound tags = getData(result);
        tags.setString(NBT_TOOL_CLASS, toolClass);
        tags.setBoolean(NBT_IS_EXAMPLE, isExample);

        NBTTagList tagList = new NBTTagList();
        NBTTagCompound partTags = new NBTTagCompound();
        part.writeToNBT(partTags);
        tagList.appendTag(partTags);
        if ("sword".equals(toolClass))
            tagList.appendTag(partTags);
        tags.setTag(NBT_MATERIALS, tagList);

        ItemPartData data = ItemPartData.instance(part);
        writeStatCache(result, PartDataList.of(data));
        return result;
    }

    private static void writeStatCache(ItemStack stack, PartDataList parts) {
        ICoreItem item = ModItems.toolClasses.get(getToolClass(stack));
        double synergy = GearData.calculateSynergyValue(parts, parts.getUniqueParts(true));
        Multimap<ItemStat, StatInstance> stats = GearData.getStatModifiers(item, parts, synergy);

        NBTTagCompound tags = new NBTTagCompound();
        for (ItemStat stat : stats.keySet()) {
            float value = stat.compute(0f, stats.get(stat));
            tags.setFloat(stat.getName().getPath(), value);
        }
        tags.setFloat("synergy", (float) synergy);
        getData(stack).setTag(NBT_STAT_CACHE, tags);
    }

    /**
     * Get the gear class this item matches.
     */
    @Nonnull
    public static String getToolClass(ItemStack stack) {
        return getData(stack).getString(NBT_TOOL_CLASS);
    }

    /**
     * Get the primary (first) part the gear head was constructed with.
     */
    @Nullable
    public static ItemPartData getPrimaryPart(ItemStack stack) {
        NBTTagCompound tags = getData(stack);
        if (!tags.hasKey(NBT_MATERIALS))
            //noinspection ConstantConditions
            return ItemPartData.instance(ModMaterials.mainWood);
        NBTTagList tagList = tags.getTagList(NBT_MATERIALS, 10);
        if (tagList.tagCount() == 0)
            //noinspection ConstantConditions
            return ItemPartData.instance(ModMaterials.mainWood);
        NBTTagCompound partTags = tagList.getCompoundTagAt(0);
        return ItemPartData.readFromNBT(partTags);
    }

    @Nullable
    public static ItemPartData getSecondaryPart(ItemStack stack) {
        NBTTagCompound tags = getData(stack);
        if (!tags.hasKey(NBT_MATERIALS))
            //noinspection ConstantConditions
            return ItemPartData.instance(ModMaterials.mainWood);
        NBTTagList tagList = tags.getTagList(NBT_MATERIALS, 10);
        if (tagList.tagCount() < 2)
            //noinspection ConstantConditions
            return ItemPartData.instance(ModMaterials.mainWood);
        NBTTagCompound partTags = tagList.getCompoundTagAt(1);
        return ItemPartData.readFromNBT(partTags);
    }

    /**
     * Get all parts the gear head was constructed with.
     */
    @Nonnull
    public static Collection<ItemPartData> getAllParts(ItemStack stack) {
        ImmutableList.Builder<ItemPartData> builder = ImmutableList.builder();
        NBTTagCompound tags = getData(stack);
        if (tags.hasKey(NBT_MATERIALS)) {
            NBTTagList tagList = tags.getTagList(NBT_MATERIALS, 10);
            for (NBTBase nbt : tagList) {
                ItemPartData data = ItemPartData.readFromNBT((NBTTagCompound) nbt);
                if (data != null) builder.add(data);
            }
        }
        return builder.build();
    }

    /**
     * Convenience method for getting NBT compound.
     */
    @Nonnull
    private static NBTTagCompound getData(ItemStack stack) {
        NBTTagCompound tags = StackHelper.getTagCompound(stack, true);
        assert tags != null;
        if (!tags.hasKey(NBT_ROOT))
            tags.setTag(NBT_ROOT, new NBTTagCompound());
        return tags.getCompoundTag(NBT_ROOT);
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        String toolClass = getToolClass(stack);
        if (!toolClass.isEmpty()) {
            return SilentGear.i18n.subText(this, toolClass);
        }
        return super.getItemStackDisplayName(stack);
    }

    public static String getModelKey(ItemStack stack) {
        ICoreTool toolItem = ModItems.toolClasses.get(getToolClass(stack));
        String toolClass = getToolClass(stack);
        ItemPartData primary = getPrimaryPart(stack);
        ItemPartData secondary = toolItem != null && toolItem.hasSwordGuard() ? getSecondaryPart(stack) : null;
        return getModelKey(toolClass, primary, secondary);
    }

    public static String getModelKey(String toolClass, @Nullable ItemPartData primary, @Nullable ItemPartData secondary) {
        return toolClass + "_head|"
                + (primary != null ? primary.getModelIndex(0) : "null")
                + (secondary != null ? "|" + secondary.getModelIndex(0) : "");
    }

    @Override
    public void addInformation(ItemStack stack, @Nullable World world, List<String> list, ITooltipFlag flag) {
        I18nHelper i18n = SilentGear.i18n;
        String toolClass = getToolClass(stack);

        //list.add(TextFormatting.AQUA + i18n.translate("item", toolClass + ".name"));

        // Materials used in crafting
        GearClientHelper.tooltipListParts(stack, list, getAllParts(stack));

        if (getData(stack).getBoolean(NBT_IS_EXAMPLE)) {
            list.add(TextFormatting.YELLOW + i18n.translate("misc", "exampleOutput1"));
            list.add(TextFormatting.YELLOW + i18n.translate("misc", "exampleOutput2"));
        }

        ICoreItem toolItem = ModItems.toolClasses.get(toolClass);
        if (toolItem != null) {
            // TODO: We're constructing a rod-less tool each time to get stats. Is that bad?
            ItemStack constructed = toolItem.construct((Item) toolItem, getAllParts(stack));
            GearData.recalculateStats(constructed);
            GearClientHelper.addInformation(constructed, world, list,
                    TooltipFlagTC.withModifierKeys(flag.isAdvanced(), true, false));
        }
    }

    @Override
    public EnumRarity getRarity(ItemStack stack) {
        // TODO Auto-generated method stub
        return super.getRarity(stack);
    }

    @Override
    public void getSubItems(CreativeTabs tab, NonNullList<ItemStack> list) {
        if (this.isInCreativeTab(tab))
            for (String toolClass : ModItems.toolClasses.keySet())
                for (PartMain part : PartRegistry.getVisibleMains())
                    list.add(getStack(toolClass, part, true));
    }

    public Collection<IRecipe> getExampleRecipes() {
        Collection<IRecipe> list = new ArrayList<>();

        //noinspection OverlyLongLambda
        ModItems.toolClasses.forEach((toolClass, item) -> {
            Ingredient blueprint = Blueprint.getBlueprintIngredientForGear(item);
            if (blueprint != null) {
                for (PartMain part : PartRegistry.getVisibleMains()) {
                    ItemStack result = getStack(toolClass, part, true);
                    NonNullList<Ingredient> ingredients = NonNullList.create();
                    ingredients.add(blueprint);
                    for (int i = 0; i < item.getConfig().getHeadCount(); ++i) {
                        ingredients.add(Ingredient.fromStacks(part.getCraftingStack()));
                    }
                    list.add(new ShapelessRecipes(SilentGear.MOD_ID, result, ingredients));
                }
            } else {
                SilentGear.log.warn("Trying to add {} example recipes, but could not find blueprint item!", item.getGearClass());
            }
        });

        return list;
    }

    @Override
    public float getStat(@Nonnull ItemStack stack, @Nonnull ItemStat stat) {
        NBTTagCompound root = getData(stack);
        if (!root.hasKey(NBT_STAT_CACHE))
            return stat.getDefaultValue();

        NBTTagCompound tags = root.getCompoundTag(NBT_STAT_CACHE);
        return tags.getFloat(stat.getName().getPath());
    }

    public static String getSubtypeKey(ItemStack stack) {
        ItemPartData part = getPrimaryPart(stack);
        return getToolClass(stack) + (part != null ? "|" + part.getModelIndex(0) : "|empty");
    }
}
