package com.rainyday;

import java.util.Random;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.ISound;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.event.sound.PlaySoundEvent17;

import cpw.mods.fml.common.eventhandler.EventPriority;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.network.FMLNetworkEvent;

/**
 * CLIENT ONLY. Swaps vanilla's rain splash sound for the current profile's.
 *
 * Vanilla plays "ambient.weather.rain" from EntityRenderer.addRainParticles about 7 times a second, at a random
 * splash near the player: volume 0.2 / pitch 1.0 outdoors, or 0.1 / 0.5 when under a roof. Forge posts
 * PlaySoundEvent17 just before any sound plays. Its 'sound' field is the ORIGINAL sound and can't be changed;
 * 'result' is what will actually play, and any mod can replace it or cancel it (null).
 *
 * Other mods (Dynamic Surroundings, for one) swap the rain sound too. So this handler:
 * - runs LAST (EventPriority.LOWEST), so RainyDay has the final say on what plays,
 * - recognises rain from the original 'sound', so it still works if another mod already swapped 'result',
 * - builds its sound from the original's volume, pitch and position, so the "under a roof" version is kept.
 */
public class RainSoundHandler {

    private static final String RAIN_DOMAIN = "minecraft";
    private static final String RAIN_PATH = "ambient.weather.rain";
    /** Debug trace is limited to one line per this many milliseconds (rain sounds play ~7 times a second). */
    private static final long TRACE_INTERVAL_MS = 5000;

    private final Random rand = new Random();
    /** Last sound name found missing, so the warning is logged once instead of 7 times a second. */
    private String warnedMissing = null;
    private long lastTrace = 0;

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onPlaySound(PlaySoundEvent17 event) {
        RainSound rain = ClientRainState.get();
        if (rain == null) return; // not on a RainyDay server: leave rain to vanilla and other mods

        ISound original = event.sound;
        ResourceLocation loc = original.getPositionedSoundLocation();
        if (!RAIN_PATH.equals(loc.getResourcePath()) || !RAIN_DOMAIN.equals(loc.getResourceDomain())) return;

        // What the other mods left us (before RainyDay's choice), for the debug trace.
        String arrivedAs = event.result == null ? "cancelled"
            : String.valueOf(event.result.getPositionedSoundLocation());

        // Sparser rain: skip a share of the splash sounds.
        if (rain.chance < 1f && rand.nextFloat() >= rain.chance) {
            event.result = null;
            trace(arrivedAs, "skipped (rainSoundChance)");
            return;
        }

        ResourceLocation target = loc; // empty rainSound = vanilla rain
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

        float[] lead = leadOffset();
        if (target == loc && rain.volume == 1f && lead == null) {
            // Vanilla rain, unchanged. Put the original back in case another mod swapped or cancelled it.
            event.result = original;
        } else {
            float dx = lead == null ? 0f : lead[0], dy = lead == null ? 0f : lead[1], dz = lead == null ? 0f : lead[2];
            event.result = new PositionedSoundRecord(
                target,
                Math.min(original.getVolume() * rain.volume, 1f),
                original.getPitch(),
                original.getXPosF() + dx,
                original.getYPosF() + dy,
                original.getZPosF() + dz);
        }
        trace(arrivedAs, String.valueOf(event.result.getPositionedSoundLocation()));
    }

    /**
     * TUNE THIS: how far ahead of you rain sounds start when you're moving fast, in seconds of your movement.
     * About half the length of the rain clips works best. Raise it if rain still trails behind on a horse,
     * lower it if rain seems to come from in front of you. 0 = off.
     */
    private static final float LEAD_SECONDS = 0.75f;

    // Speeds in blocks per tick (20 ticks = 1 second).
    /** At or below sprinting (5.6 blocks/s) nothing moves, so walking around sounds exactly as before. */
    private static final double LEAD_START_SPEED = 0.28;
    /** The shift fades in between sprinting and this speed (8 blocks/s), so there's no sudden jump. */
    private static final double LEAD_FULL_SPEED = 0.40;
    /** Faster than this (80 blocks/s) is a teleport or a respawn, not travel: no shift. */
    private static final double TELEPORT_SPEED = 4.0;
    /** Never move a sound further than this many blocks. */
    private static final double MAX_LEAD_BLOCKS = 12.0;

    /**
     * How far to move a rain sound ahead of the player, or null for no change.
     *
     * Rain sounds stay where they start, about 9 blocks around you, and fade out over 16 blocks. A fast horse
     * covers ~14 blocks a second, so a 1-2 second clip ends up behind you before it finishes and the rain seems
     * to trail you. Starting each sound ahead of you by half a clip's worth of travel keeps it around you.
     * Movement is this tick's change in position (posX - lastTickPosX), which also works while riding.
     */
    private static float[] leadOffset() {
        if (LEAD_SECONDS <= 0f) return null;
        Entity viewer = Minecraft.getMinecraft().renderViewEntity;
        if (viewer == null) return null;

        double vx = viewer.posX - viewer.lastTickPosX;
        double vy = viewer.posY - viewer.lastTickPosY;
        double vz = viewer.posZ - viewer.lastTickPosZ;
        double speed = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (speed <= LEAD_START_SPEED || speed > TELEPORT_SPEED) return null;

        double fade = Math.min(1.0, (speed - LEAD_START_SPEED) / (LEAD_FULL_SPEED - LEAD_START_SPEED));
        double ticks = LEAD_SECONDS * 20.0 * fade;
        double length = speed * ticks;
        if (length > MAX_LEAD_BLOCKS) ticks *= MAX_LEAD_BLOCKS / length;
        return new float[] { (float) (vx * ticks), (float) (vy * ticks), (float) (vz * ticks) };
    }

    /**
     * With debug on, shows what other mods did to the rain sound and what RainyDay played, at most once
     * every 5 seconds. "arrived as" differing from minecraft:ambient.weather.rain means another mod swapped it.
     */
    private void trace(String arrivedAs, String playedAs) {
        if (!Config.debug) return;
        long now = System.currentTimeMillis();
        if (now - lastTrace < TRACE_INTERVAL_MS) return;
        lastTrace = now;
        RainyDay.LOG.info(
            "Rain sound trace ({}): arrived as {}{}, playing {}",
            ClientRainState.profile(),
            arrivedAs,
            arrivedAs.equals(RAIN_DOMAIN + ":" + RAIN_PATH) ? "" : " (changed by another mod)",
            playedAs);
    }

    /** CLIENT ONLY, registered on FML's bus: leaving a server goes back to vanilla rain. */
    public static class Disconnect {

        @SubscribeEvent
        public void onDisconnect(FMLNetworkEvent.ClientDisconnectionFromServerEvent event) {
            ClientRainState.clear();
        }
    }
}
