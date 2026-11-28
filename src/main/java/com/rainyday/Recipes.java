package com.rainyday;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraftforge.oredict.OreDictionary;
import net.minecraftforge.oredict.ShapedOreRecipe;

import cpw.mods.fml.common.Loader;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * Weather TV recipes. With GregTech 5 Unofficial installed, only the GT recipe is added; otherwise the vanilla one.
 *
 * GT parts are found by ore dictionary name and the casing by its registered block name, so RainyDay needs no GT
 * code to compile or run. If GT is installed but a part can't be found, RainyDay logs a warning and falls back to the
 * vanilla recipe so the TV is never uncraftable.
 */
public final class Recipes {

    private static final String GT_MODID = "gregtech";
    private static final String GT_CASINGS = "gt.blockcasings";
    private static final int CASING_LV_META = 1;

    // Ore dictionary names used by GT5u.
    private static final String[] SCREWS = { "screwIron", "screwCastIron" };
    private static final String CIRCUIT_LV = "circuitBasic";
    private static final String VACUUM_TUBE = "circuitPrimitive";
    private static final String GLASS_PANE = "paneGlass";

    private Recipes() {}

    /** Call in postInit, after every mod has registered its items and ore dictionary entries. */
    public static void register() {
        if (Loader.isModLoaded(GT_MODID) && addGregTechRecipe()) {
            RainyDay.LOG.info("GregTech found: using the GregTech Weather TV recipe.");
            return;
        }
        addVanillaRecipe();
    }

    /**
     * S C S S = iron (or cast iron) screw, C = LV circuit
     * V P V V = vacuum tube, P = any glass pane
     * S M S M = LV machine casing
     */
    private static boolean addGregTechRecipe() {
        Block casings = GameRegistry.findBlock(GT_MODID, GT_CASINGS);
        if (casings == null) return missing("the LV Machine Casing (" + GT_MODID + ":" + GT_CASINGS + ")");

        List<ItemStack> screws = new ArrayList<ItemStack>();
        for (String name : SCREWS) screws.addAll(OreDictionary.getOres(name, false));
        if (screws.isEmpty()) return missing("iron screws (screwIron / screwCastIron)");
        if (OreDictionary.getOres(CIRCUIT_LV, false)
            .isEmpty()) return missing("LV circuits (" + CIRCUIT_LV + ")");
        if (OreDictionary.getOres(VACUUM_TUBE, false)
            .isEmpty()) return missing("vacuum tubes (" + VACUUM_TUBE + ")");

        ShapedOreRecipe recipe = new ShapedOreRecipe(
            new ItemStack(RainyDay.weatherTV),
            "SCS",
            "VPV",
            "SMS",
            'S',
            SCREWS[0], // placeholder, swapped for the combined screw list below
            'C',
            CIRCUIT_LV,
            'V',
            VACUUM_TUBE,
            'P',
            GLASS_PANE,
            'M',
            new ItemStack(casings, 1, CASING_LV_META));

        // A ShapedOreRecipe slot can name only one ore dictionary entry, but it accepts a list of items, so the
        // screw slots get both screw lists merged. NEI shows and cycles through these lists like any ore slot.
        Object[] input = recipe.getInput();
        for (int slot : new int[] { 0, 2, 6, 8 }) input[slot] = screws;

        GameRegistry.addRecipe(recipe);
        return true;
    }

    private static boolean missing(String what) {
        RainyDay.LOG.warn("GregTech is installed but {} wasn't found; using the vanilla Weather TV recipe.", what);
        return false;
    }

    /**
     * I I I I = iron ingot
     * I P I P = glass pane
     * I R I R = redstone
     */
    private static void addVanillaRecipe() {
        GameRegistry.addShapedRecipe(
            new ItemStack(RainyDay.weatherTV),
            "III",
            "IPI",
            "IRI",
            'I',
            Items.iron_ingot,
            'P',
            Blocks.glass_pane,
            'R',
            Items.redstone);
    }
}
