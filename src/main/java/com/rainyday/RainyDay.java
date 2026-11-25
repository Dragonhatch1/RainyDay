package com.rainyday;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;

/**
 * RainyDay: it always rains, and the weather randomly switches between configurable rain profiles
 * (drizzle, steady rain, heavy rain, thunderstorm, ...).
 *
 * Server-side only. Clients don't need it; vanilla already syncs rain and thunder strength to them.
 */
@Mod(
    modid = RainyDay.MODID,
    name = "RainyDay",
    version = Tags.VERSION,
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class RainyDay {

    public static final String MODID = "rainyday";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // TickEvents are fired on the FML bus, not MinecraftForge.EVENT_BUS.
        FMLCommonHandler.instance()
            .bus()
            .register(new WeatherHandler());
        for (RainProfile p : Config.profiles) LOG.info("Rain profile: {}", p);
    }
}
