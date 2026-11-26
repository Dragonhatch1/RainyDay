package com.rainyday;

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
    }
}
