package com.rainyday;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.RenderGameOverlayEvent;

import org.lwjgl.opengl.GL11;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * CLIENT ONLY. Draws the current weather profile's icon on the HUD.
 *
 * Icons are assets/rainyday/textures/gui/weather/<profile>.png (32x32). A profile without an icon file shows
 * nothing. The position is stored as an offset from the screen corner nearest the icon (Config.hudAnchor),
 * so it stays in place when the window size or GUI scale changes.
 */
public class HudIcon {

    /** Icon size in GUI pixels at scale 1.0 (an item icon is 16). */
    public static final int BASE_SIZE = 16;
    /** Whether each icon file exists, so a missing one isn't looked up every frame. */
    private static final Map<String, Boolean> exists = new HashMap<>();

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) return;

        Minecraft mc = Minecraft.getMinecraft();
        // Hidden with F1, with the F3 debug screen, and while any menu or screen is open.
        if (mc.gameSettings.hideGUI || mc.gameSettings.showDebugInfo || mc.currentScreen != null) return;

        String profile = ClientRainState.profile();
        if (profile.isEmpty()) return; // not on a RainyDay server, or this dimension isn't managed

        int screenW = event.resolution.getScaledWidth();
        int screenH = event.resolution.getScaledHeight();
        draw(profile, iconX(screenW), iconY(screenH), iconSize());
    }

    // ---- Position, shared with the placement screen ----

    public static int iconSize() {
        return Math.round(BASE_SIZE * Config.hudScale);
    }

    /** Left edge of the icon on a screen this wide (GUI pixels). */
    public static int iconX(int screenW) {
        int x = Config.hudAnchor.endsWith("right") ? screenW - Config.hudX - iconSize() : Config.hudX;
        return clamp(x, 0, Math.max(0, screenW - iconSize()));
    }

    /** Top edge of the icon on a screen this tall (GUI pixels). */
    public static int iconY(int screenH) {
        int y = Config.hudAnchor.startsWith("bottom") ? screenH - Config.hudY - iconSize() : Config.hudY;
        return clamp(y, 0, Math.max(0, screenH - iconSize()));
    }

    /**
     * Stores an icon placed with its top-left corner at (x, y): picks the screen corner nearest the icon's
     * center and saves the distance from that corner to the icon's nearest edges.
     */
    public static void setPosition(int x, int y, int screenW, int screenH) {
        int size = iconSize();
        x = clamp(x, 0, Math.max(0, screenW - size));
        y = clamp(y, 0, Math.max(0, screenH - size));
        boolean right = x + size / 2 > screenW / 2;
        boolean bottom = y + size / 2 > screenH / 2;
        Config.hudAnchor = (bottom ? "bottom" : "top") + "_" + (right ? "right" : "left");
        Config.hudX = right ? screenW - x - size : x;
        Config.hudY = bottom ? screenH - y - size : y;
    }

    // ---- Drawing ----

    /** Draws a profile's icon at (x, y) with the given size in GUI pixels. Returns false if it has no icon. */
    public static boolean draw(String profile, int x, int y, int size) {
        ResourceLocation tex = textureFor(profile);
        if (tex == null) return false;

        Minecraft mc = Minecraft.getMinecraft();
        mc.getTextureManager()
            .bindTexture(tex);
        GL11.glEnable(GL11.GL_BLEND);
        OpenGlHelper.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GL11.glColor4f(1f, 1f, 1f, 1f);
        // Draws the whole 32x32 image squeezed or stretched into size x size.
        Gui.func_146110_a(x, y, 0f, 0f, size, size, size, size);
        GL11.glDisable(GL11.GL_BLEND);
        return true;
    }

    /** The icon for a profile, or null if there's no image file for it. */
    public static ResourceLocation textureFor(String profile) {
        ResourceLocation tex = new ResourceLocation(RainyDay.MODID, "textures/gui/weather/" + profile + ".png");
        Boolean found = exists.get(profile);
        if (found == null) {
            try {
                Minecraft.getMinecraft()
                    .getResourceManager()
                    .getResource(tex);
                found = true;
            } catch (Exception e) {
                found = false;
                RainyDay.LOG.info("No HUD icon for profile '{}' ({}); none will be shown.", profile, tex);
            }
            exists.put(profile, found);
        }
        return found ? tex : null;
    }

    /** Forget which icon files exist, e.g. after resource packs change. */
    public static void clearCache() {
        exists.clear();
    }

    static int clamp(int v, int min, int max) {
        return Math.max(min, Math.min(max, v));
    }
}
