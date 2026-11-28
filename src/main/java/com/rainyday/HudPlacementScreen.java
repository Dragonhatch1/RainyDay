package com.rainyday;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;

import org.lwjgl.input.Mouse;

/**
 * CLIENT ONLY. Opened with the RainyDay HUD key (default H): drag the weather icon where you want it, mouse wheel
 * or +/- to resize it. Saved to rainyday.cfg when the screen closes (Done or Esc).
 */
public class HudPlacementScreen extends GuiScreen {

    private static final float SCALE_STEP = 0.25f;
    private static final int BUTTON_DONE = 0, BUTTON_RESET = 1, BUTTON_SMALLER = 2, BUTTON_BIGGER = 3;

    private boolean dragging = false;
    /** Where inside the icon the mouse grabbed it, so it doesn't jump to the cursor. */
    private int grabX, grabY;

    @Override
    @SuppressWarnings("unchecked")
    public void initGui() {
        buttonList.clear();
        int cx = width / 2, y = height / 2 + 30;
        buttonList.add(new GuiButton(BUTTON_SMALLER, cx - 100, y, 20, 20, "-"));
        buttonList.add(new GuiButton(BUTTON_BIGGER, cx - 76, y, 20, 20, "+"));
        buttonList.add(new GuiButton(BUTTON_RESET, cx - 52, y, 72, 20, "Reset"));
        buttonList.add(new GuiButton(BUTTON_DONE, cx + 24, y, 76, 20, "Done"));
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        int size = HudIcon.iconSize();
        int x = HudIcon.iconX(width), y = HudIcon.iconY(height);

        // Outline so the icon's area is visible even for a profile without an icon.
        int outline = dragging ? 0xFFFFFF55 : 0xAAFFFFFF;
        Gui.drawRect(x - 1, y - 1, x + size + 1, y, outline);
        Gui.drawRect(x - 1, y + size, x + size + 1, y + size + 1, outline);
        Gui.drawRect(x - 1, y, x, y + size, outline);
        Gui.drawRect(x + size, y, x + size + 1, y + size, outline);

        String profile = shownProfile();
        if (!HudIcon.draw(profile, x, y, size)) Gui.drawRect(x, y, x + size, y + size, 0x40FFFFFF);

        int cx = width / 2, ty = height / 2 - 30;
        drawCenteredString(fontRendererObj, "RainyDay weather icon", cx, ty, 0xFFFFFF);
        drawCenteredString(fontRendererObj, "Drag the icon to move it. Mouse wheel or +/- to resize.", cx, ty + 12, 0xAAAAAA);
        drawCenteredString(
            fontRendererObj,
            String.format(
                "Showing: %s   Size: %.2fx   From %s corner: %d, %d",
                profile,
                Config.hudScale,
                Config.hudAnchor.replace('_', ' '),
                Config.hudX,
                Config.hudY),
            cx,
            ty + 24,
            0xAAAAAA);

        super.drawScreen(mouseX, mouseY, partialTicks); // buttons
    }

    /** The active profile's icon, or the first profile's if there's no active one (e.g. single-player menu). */
    private static String shownProfile() {
        String p = ClientRainState.profile();
        if (!p.isEmpty()) return p;
        return Config.profiles.isEmpty() ? "" : Config.profiles.get(0).name;
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int button) {
        int size = HudIcon.iconSize();
        int x = HudIcon.iconX(width), y = HudIcon.iconY(height);
        if (button == 0 && mouseX >= x && mouseX < x + size && mouseY >= y && mouseY < y + size) {
            dragging = true;
            grabX = mouseX - x;
            grabY = mouseY - y;
            return;
        }
        super.mouseClicked(mouseX, mouseY, button); // buttons
    }

    @Override
    protected void mouseClickMove(int mouseX, int mouseY, int button, long timeSinceClick) {
        if (dragging) HudIcon.setPosition(mouseX - grabX, mouseY - grabY, width, height);
    }

    @Override
    protected void mouseMovedOrUp(int mouseX, int mouseY, int state) {
        dragging = false; // in 1.7.10 this is only called when a mouse button is released
        super.mouseMovedOrUp(mouseX, mouseY, state);
    }

    @Override
    public void handleMouseInput() {
        super.handleMouseInput();
        int wheel = Mouse.getEventDWheel();
        if (wheel != 0) resize(wheel > 0 ? SCALE_STEP : -SCALE_STEP);
    }

    @Override
    protected void actionPerformed(GuiButton button) {
        switch (button.id) {
            case BUTTON_DONE:
                mc.displayGuiScreen(null);
                break;
            case BUTTON_RESET:
                Config.hudAnchor = "top_left";
                Config.hudX = 4;
                Config.hudY = 4;
                Config.hudScale = 1.0f;
                break;
            case BUTTON_SMALLER:
                resize(-SCALE_STEP);
                break;
            case BUTTON_BIGGER:
                resize(SCALE_STEP);
                break;
        }
    }

    /** Changes the size, keeping the icon's center in place. */
    private void resize(float delta) {
        float newScale = Math.max(Config.HUD_SCALE_MIN, Math.min(Config.HUD_SCALE_MAX, Config.hudScale + delta));
        if (newScale == Config.hudScale) return;
        int oldSize = HudIcon.iconSize();
        int centerX = HudIcon.iconX(width) + oldSize / 2, centerY = HudIcon.iconY(height) + oldSize / 2;
        Config.hudScale = newScale;
        int size = HudIcon.iconSize();
        HudIcon.setPosition(centerX - size / 2, centerY - size / 2, width, height);
    }

    @Override
    public void onGuiClosed() {
        Config.saveHud();
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
