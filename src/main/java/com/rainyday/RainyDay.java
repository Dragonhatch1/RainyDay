package com.rainyday;

import net.minecraft.block.Block;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.registry.GameRegistry;

/**
 * RainyDay: it always rains, and the weather randomly switches between configurable rain profiles
 * (drizzle, steady rain, heavy rain, thunderstorm, ...).
 *
 * The weather runs on the server and is synced to players by vanilla. Because RainyDay adds a block (the Weather
 * TV), players need it installed to join, so Forge's default "same mod on both sides" check applies.
 */
@Mod(modid = RainyDay.MODID, name = "RainyDay", version = Tags.VERSION, acceptedMinecraftVersions = "[1.7.10]")
public class RainyDay {

    public static final String MODID = "rainyday";
    public static final Logger LOG = LogManager.getLogger(MODID);

    @SidedProxy(clientSide = "com.rainyday.ClientProxy", serverSide = "com.rainyday.CommonProxy")
    public static CommonProxy proxy;

    public static Block weatherTV;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        Config.load(event.getSuggestedConfigurationFile());
        Network.init();

        weatherTV = new BlockWeatherTV();
        GameRegistry.registerBlock(weatherTV, "weather_tv");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // TickEvents are fired on the FML bus, not MinecraftForge.EVENT_BUS.
        FMLCommonHandler.instance()
            .bus()
            .register(new WeatherHandler());
        // Tells players' games the current profile when they join, respawn or change dimension.
        FMLCommonHandler.instance()
            .bus()
            .register(new Network.PlayerSync());
        proxy.init(); // players' games: rain sound swapping

        // Weather TV: iron casing, glass pane screen, redstone underneath.
        //   I I I
        //   I P I
        //   I R I
        GameRegistry.addShapedRecipe(
            new ItemStack(weatherTV),
            "III",
            "IPI",
            "IRI",
            'I',
            Items.iron_ingot,
            'P',
            Blocks.glass_pane,
            'R',
            Items.redstone);
        for (RainProfile p : Config.profiles) LOG.info("Rain profile: {}", p);
    }
}
