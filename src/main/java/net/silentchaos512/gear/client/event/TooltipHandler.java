package net.silentchaos512.gear.client.event;

import net.minecraft.item.ItemStack;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.TranslationTextComponent;
import net.minecraftforge.event.entity.player.ItemTooltipEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.silentchaos512.gear.SilentGear;
import net.silentchaos512.gear.api.parts.IGearPart;
import net.silentchaos512.gear.api.parts.IPartMaterial;
import net.silentchaos512.gear.api.parts.MaterialGrade;
import net.silentchaos512.gear.api.parts.PartType;
import net.silentchaos512.gear.api.stats.ItemStat;
import net.silentchaos512.gear.api.stats.ItemStats;
import net.silentchaos512.gear.api.stats.StatInstance;
import net.silentchaos512.gear.api.traits.ITrait;
import net.silentchaos512.gear.client.KeyTracker;
import net.silentchaos512.gear.parts.PartData;
import net.silentchaos512.gear.parts.PartManager;
import net.silentchaos512.lib.event.ClientTicks;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TooltipHandler {
    public static final TooltipHandler INSTANCE = new TooltipHandler();

    // Display a single trait and cycling through the list. Main problem with this is it affects
    // JEI's tooltip cache. When disabled, you can search for parts with certain traits.
    private static final boolean TRAIT_DISPLAY_CYCLE = false;
    private static final boolean DETAILED_MATERIAL_INFO = false;

    private TooltipHandler() {}

    @SubscribeEvent
    public void onTooltip(ItemTooltipEvent event) {
        /*
        LoaderState state = Loader.instance().getLoaderState();
        if (state == LoaderState.INITIALIZATION || state == LoaderState.SERVER_ABOUT_TO_START || state == LoaderState.SERVER_STOPPING) {
            // Skip tooltips during block/item remapping
            // JEI tooltip caches are done in AVAILABLE, in-game is SERVER_STARTED
            return;
        }
        */

        ItemStack stack = event.getItemStack();
        IGearPart part = !stack.isEmpty() ? PartManager.from(stack) : null;

        if (part != null /*&& !part.isBlacklisted(stack)*/) {
            onPartTooltip(event, stack, PartData.of(part));
            return;
        }

        // Nerfed gear?
        /* FIXME
        ResourceLocation name = stack.getItem().getRegistryName();
        if (name != null && Config.nerfedGear.contains(name.toString())) {
            event.getToolTip().add(1, new TranslationTextComponent("misc.silentgear.poorlyMade")
                    .applyTextStyle(TextFormatting.RED));
        }
        */
    }

    private static void onPartTooltip(ItemTooltipEvent event, ItemStack stack, PartData partData) {
        IGearPart part = partData.getPart();

        // Type, tier
        event.getToolTip().add(part.getType().getDisplayName(part.getTier()).applyTextStyle(TextFormatting.GREEN));

        if (event.getFlags().isAdvanced() && DETAILED_MATERIAL_INFO && SilentGear.isDevBuild()) {
            addDetailedMaterialInfo(event, part);
        }

        // Traits
        Map<ITrait, Integer> traits = partData.getTraits();
        List<ITrait> visibleTraits = traits.keySet().stream()
                .filter(t -> t.showInTooltip(event.getFlags()))
                .collect(Collectors.toList());
        int numTraits = visibleTraits.size();
        int traitIndex = getTraitDisplayIndex(numTraits);
        int i = 0;
        for (ITrait trait : visibleTraits) {
            if (traitIndex < 0 || traitIndex == i) {
                final int level = traits.get(trait);
                trait.addInformation(level, event.getToolTip(), event.getFlags());
            }
            ++i;
        }

        MaterialGrade grade = MaterialGrade.fromStack(stack);
        if (KeyTracker.isControlDown()) {
            if (part.getType() == PartType.MAIN) {
                getGradeLine(event, grade);
            }
            event.getToolTip().add(new TranslationTextComponent("misc.silentgear.tooltip.stats")
                    .applyTextStyle(TextFormatting.GOLD)
                    .appendSibling(new StringTextComponent(" (Silent Gear)")
                            .applyTextStyle(TextFormatting.RESET)
                            .applyTextStyle(TextFormatting.ITALIC)));
            getPartStatLines(event, stack, part);
        } else {
            if (grade != MaterialGrade.NONE && part.getType() == PartType.MAIN) {
                getGradeLine(event, grade);
            }
            event.getToolTip().add(new TranslationTextComponent("misc.silentgear.tooltip.ctrlForStats").applyTextStyle(TextFormatting.GOLD));
        }
    }

    private static void addDetailedMaterialInfo(ItemTooltipEvent event, IGearPart part) {
        IPartMaterial mat = part.getMaterials();
        if (mat.getItem() != null)
            event.getToolTip().add(new StringTextComponent("item: " + mat.getItem().asItem().getRegistryName()));
        if (mat.getTag() != null)
            event.getToolTip().add(new StringTextComponent("tag: " + mat.getTag().getId()));
        if (mat.getSmallItem() != null)
            event.getToolTip().add(new StringTextComponent("itemSmall: " + mat.getSmallItem().asItem().getRegistryName()));
        if (mat.getSmallTag() != null)
            event.getToolTip().add(new StringTextComponent("tagSmall: " + mat.getSmallTag().getId()));
    }

    private static int getTraitDisplayIndex(int numTraits) {
        if (!TRAIT_DISPLAY_CYCLE || KeyTracker.isControlDown() || numTraits == 0)
            return -1;
        return ClientTicks.ticksInGame() / 20 % numTraits;
    }

    private static void getGradeLine(ItemTooltipEvent event, MaterialGrade grade) {
        event.getToolTip().add(grade.getDisplayName().applyTextStyle(TextFormatting.AQUA));
    }

    private static final Pattern REGEX_TRIM_TO_INT = Pattern.compile("\\.0+$");
    private static final Pattern REGEX_REMOVE_TRAILING_ZEROS = Pattern.compile("0+$");

    private static void getPartStatLines(ItemTooltipEvent event, ItemStack stack, IGearPart part) {
        PartData partData = PartData.of(part, MaterialGrade.fromStack(stack), stack);
        for (ItemStat stat : ItemStat.ALL_STATS.values()) {
            Collection<StatInstance> modifiers = part.getStatModifiers(stat, partData);

            if (!modifiers.isEmpty()) {
                StatInstance inst = stat.computeForDisplay(0, partData.getGrade(), modifiers);
                if (inst.shouldList(part, stat, event.getFlags().isAdvanced())) {
                    boolean isZero = inst.getValue() == 0;
                    TextFormatting nameColor = isZero ? TextFormatting.DARK_GRAY : stat.displayColor;
                    TextFormatting statColor = isZero ? TextFormatting.DARK_GRAY : TextFormatting.WHITE;
                    ITextComponent nameStr = stat.getDisplayName().applyTextStyle(nameColor);
                    int decimalPlaces = stat.displayAsInt && inst.getOp() != StatInstance.Operation.MUL1 && inst.getOp() != StatInstance.Operation.MUL2 ? 0 : 2;

                    String statStr = statColor + REGEX_TRIM_TO_INT.matcher(inst.formattedString(decimalPlaces, false)).replaceFirst("");
                    if (statStr.contains("."))
                        statStr = REGEX_REMOVE_TRAILING_ZEROS.matcher(statStr).replaceFirst("");
                    if (modifiers.size() > 1)
                        statStr += "*";
                    if (stat == ItemStats.ARMOR_DURABILITY)
                        statStr += "x";

                    event.getToolTip().add(new StringTextComponent("- ").appendSibling(new TranslationTextComponent("stat.silentgear.displayFormat", nameStr, statStr)));
                }
            }
        }
    }
}
