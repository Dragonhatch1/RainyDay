package com.rainyday;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraftforge.common.MinecraftForge;

import cpw.mods.fml.common.FMLCommonHandler;

/** Only loaded on players' games (see @SidedProxy in RainyDay), so client-only classes are safe here. */
public class ClientProxy extends CommonProxy {

    @Override
    public void init() {
        // Sound event is on Forge's bus, disconnect event on FML's: one listener per bus.
        MinecraftForge.EVENT_BUS.register(new RainSoundHandler());
        FMLCommonHandler.instance()
            .bus()
            .register(new RainSoundHandler.Disconnect());

        // Weather icon on the HUD (Forge's bus) and the key that opens its placement screen (FML's bus).
        MinecraftForge.EVENT_BUS.register(new HudIcon());
        FMLCommonHandler.instance()
            .bus()
            .register(new HudKeyHandler());
        // Re-check which icon files exist after F3+T or a resource pack change.
        ((IReloadableResourceManager) Minecraft.getMinecraft()
            .getResourceManager()).registerReloadListener(manager -> HudIcon.clearCache());
    }
}
