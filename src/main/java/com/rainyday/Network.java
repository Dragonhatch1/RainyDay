package com.rainyday;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.world.World;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

/** RainyDay's network channel: tells players' games which profile is active, for per-profile rain sounds. */
public final class Network {

    public static final SimpleNetworkWrapper CHANNEL = NetworkRegistry.INSTANCE.newSimpleChannel(RainyDay.MODID);

    private Network() {}

    /** Called on both sides during startup; the IDs must match on server and player. */
    public static void init() {
        CHANNEL.registerMessage(ProfileMessage.Handler.class, ProfileMessage.class, 0, Side.CLIENT);
    }

    /** A profile just started in this dimension: tell everyone there. */
    public static void sendProfileToDimension(int dimension, RainProfile profile) {
        CHANNEL.sendToDimension(new ProfileMessage(profile.name, profile.rainSound), dimension);
    }

    /** Tell one player what's active in the dimension they're in now (or "nothing" if it isn't managed). */
    public static void sendCurrentTo(EntityPlayerMP player) {
        World world = player.worldObj;
        RainProfile profile = null;
        if (!world.provider.hasNoSky && Config.dimensions.contains(world.provider.dimensionId)) {
            profile = Config.byName(WeatherHandler.getState(world).profile);
        }
        // If the profile isn't known yet (world's first tick), the broadcast when it starts covers this player.
        CHANNEL.sendTo(
            profile == null ? new ProfileMessage("", RainSound.VANILLA)
                : new ProfileMessage(profile.name, profile.rainSound),
            player);
    }

    /** Server side, registered on FML's bus: keeps each player's game in sync as they move around. */
    public static class PlayerSync {

        @SubscribeEvent
        public void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
            send(event.player);
        }

        @SubscribeEvent
        public void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
            send(event.player);
        }

        @SubscribeEvent
        public void onChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
            send(event.player);
        }

        private static void send(EntityPlayer player) {
            if (player instanceof EntityPlayerMP) sendCurrentTo((EntityPlayerMP) player);
        }
    }
}
