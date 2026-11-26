package com.rainyday;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * CLIENT ONLY. Swaps vanilla's rain splash sound for the current profile's.
 *
 * Vanilla plays "ambient.weather.rain" from EntityRenderer.addRainParticles about 7 times a second, at a random
 * splash near the player: volume 0.2 / pitch 1.0 outdoors, or 0.1 / 0.5 when under a roof. Forge posts
 * PlaySoundEvent17 just before any sound plays, and lets us replace it (result) or cancel it (result = null).
 * We keep the same position, pitch and base volume, so the "muffled under a roof" version still works.
 */
public class RainSoundHandler {

    private static final String RAIN_DOMAIN = "minecraft";
    private static final String RAIN_PATH = "ambient.weather.rain";

    private final Random rand = new Random();
    /** Last sound name found missing, so the warning is logged once instead of 7 times a second. */
    private String warnedMissing = null;

    @SubscribeEvent
    public void onPlaySound(PlaySoundEvent17 event) {
        RainSound rain = ClientRainState.get();
        if (rain == null || event.result == null) return; // not on a RainyDay server, or already cancelled

        ISound sound = event.result;
        ResourceLocation loc = sound.getPositionedSoundLocation();
        if (!RAIN_PATH.equals(loc.getResourcePath()) || !RAIN_DOMAIN.equals(loc.getResourceDomain())) return;
        if (rain.isVanilla()) return;

        // Sparser rain: skip a share of the splash sounds.
        if (rain.chance < 1f && rand.nextFloat() >= rain.chance) {
            event.result = null;
            return;
        }

        ResourceLocation target = loc;
        if (!rain.sound.isEmpty()) {
            ResourceLocation custom = new ResourceLocation(rain.sound);
            // A missing sound (typo, or files not installed) would play nothing at all: keep vanilla rain instead.
            if (Minecraft.getMinecraft()
                .getSoundHandler()
                .getSound(custom) != null) {
                target = custom;
            } else if (!rain.sound.equals(warnedMissing)) {
                warnedMissing = rain.sound;
                RainyDay.LOG.warn("Rain sound '{}' isn't defined in any sounds.json; using vanilla rain.", rain.sound);
            }
        }

        if (target == loc && rain.volume == 1f) return; // nothing to change

        event.result = new PositionedSoundRecord(
            target,
            Math.min(sound.getVolume() * rain.volume, 1f),
            sound.getPitch(),
            sound.getXPosF(),
            sound.getYPosF(),
            sound.getZPosF());
    }

    /** CLIENT ONLY, registered on FML's bus: leaving a server goes back to vanilla rain. */
    public static class Disconnect {

        @SubscribeEvent
        public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
            ClientRainState.clear();
        }
    }
}
