package com.rainyday;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.KeyBinding;

import org.lwjgl.input.Keyboard;

import cpw.mods.fml.client.registry.ClientRegistry;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.InputEvent;

/** CLIENT ONLY, registered on FML's bus. The key that opens the HUD icon placement screen (default H). */
public class HudKeyHandler {

    public static final KeyBinding OPEN_PLACEMENT = new KeyBinding(
        "key.rainyday.hud",
        Keyboard.KEY_H,
        "key.categories.rainyday");

    public HudKeyHandler() {
        ClientRegistry.registerKeyBinding(OPEN_PLACEMENT); // shows up in Options > Controls, rebindable
    }

    @SubscribeEvent
    public void onKey(InputEvent.KeyInputEvent event) {
        Minecraft mc = Minecraft.getMinecraft();
        if (OPEN_PLACEMENT.isPressed() && mc.currentScreen == null) {
            mc.displayGuiScreen(new HudPlacementScreen());
        }
    }
}
