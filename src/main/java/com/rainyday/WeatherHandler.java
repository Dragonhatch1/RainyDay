package com.rainyday;

import java.util.Random;

import net.minecraft.world.World;
import net.minecraft.world.storage.WorldInfo;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;
import cpw.mods.fml.relauncher.Side;

/**
 * How it works (see World.updateWeatherBody in vanilla):
 * Every tick, vanilla nudges rain strength and thunder strength 0.01 toward 1 if its "raining"/"thundering"
 * switch is on, or toward 0 if it's off, then sends any change to players.
 *
 * RainyDay runs just before that each tick and flips those switches so vanilla nudges the strengths toward
 * the current profile's rolled targets. Vanilla keeps doing the fading, lightning, snow and syncing to
 * players. Vanilla's own weather timers are held high so they never interfere.
 */
public class WeatherHandler {

    private static final int HOLD_TICKS = 1_000_000;

    private final Random rand = new Random();
    private final AmbientSoundPlayer sounds = new AmbientSoundPlayer();

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.SERVER) return;

        World world = event.world;
        if (world.provider.hasNoSky) return;
        if (!Config.dimensions.contains(world.provider.dimensionId)) return;

        WorldInfo info = world.getWorldInfo();
        WeatherState state = getState(world);

        // Vanilla's rain timer is held high every tick. If it's 0, sleeping or /weather clear just reset
        // the weather: treat that as a reroll.
        boolean reset = info.getRainTime() <= 0;
        info.setRainTime(HOLD_TICKS);
        info.setThunderTime(HOLD_TICKS);

        // Start a new profile when the current one runs out, the weather was reset, the profile was removed
        // from the config, or the save is from an older version without rolled strengths (rain 0).
        RainProfile profile = Config.byName(state.profile);
        if (profile == null || reset || state.rain <= 0.2f || --state.ticksLeft <= 0) {
            profile = pick();
            startProfile(world, state, profile);
        }
        state.markDirty();

        info.setRaining(stepUp(world.rainingStrength, state.rain));
        info.setThundering(stepUp(world.thunderingStrength, state.thunder));

        sounds.tick(world, profile.sound);
    }

    private void startProfile(World world, WeatherState state, RainProfile profile) {
        state.profile = profile.name;
        state.rain = profile.rollRain(rand);
        state.thunder = profile.rollThunder(rand);
        state.ticksLeft = profile.rollDurationTicks(rand);
        sounds.reset();

        if (Config.debug) {
            RainyDay.LOG.info(
                "Starting {} (rain {}, thunder {}, for {} min) in dimension {}",
                profile.name,
                String.format("%.2f", state.rain),
                String.format("%.2f", state.thunder),
                String.format("%.1f", state.ticksLeft / 1200f),
                world.provider.dimensionId);
        }
    }

    /** Should vanilla push this strength up (true) or down (false) this tick to approach the target? */
    private static boolean stepUp(float current, float target) {
        if (target >= 1f) return true; // pinned at max, vanilla clamps it there
        if (target <= 0f) return false; // pinned at 0
        return current < target; // in between: settles within 0.01 of the target
    }

    private RainProfile pick() {
        int total = 0;
        for (RainProfile p : Config.profiles) total += p.weight;
        int roll = rand.nextInt(total);
        for (RainProfile p : Config.profiles) {
            roll -= p.weight;
            if (roll < 0) return p;
        }
        return Config.profiles.get(0);
    }

    private static WeatherState getState(World world) {
        WeatherState state = (WeatherState) world.perWorldStorage.loadData(WeatherState.class, WeatherState.NAME);
        if (state == null) {
            state = new WeatherState(WeatherState.NAME);
            world.perWorldStorage.setData(WeatherState.NAME, state);
        }
        return state;
    }
}
