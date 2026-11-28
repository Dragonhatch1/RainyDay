package com.rainyday;

import java.util.Collections;
import java.util.Random;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
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
 *
 * Profiles form a chain in the order of the config's 'profiles' list (drizzle -> steady -> heavy -> storm).
 * When one ends it stays, goes back one or goes forward one (Config.chanceBack/Stay/Forward). The first
 * profile can't go back (that share goes forward); the last one wraps forward to the first.
 *
 * The chain is planned one step ahead: whenever a profile starts, the following profile and its length are
 * rolled right away and saved (WeatherState.nextProfile / nextTicks), so the Weather TV can forecast it.
 * When the current profile ends, that planned profile is the one that starts.
 */
public class WeatherHandler {

    private static final int HOLD_TICKS = 1_000_000;

    private final Random rand = new Random();
    private final AmbientSoundPlayer sounds = new AmbientSoundPlayer();
    /** Worlds where every player was asleep at the start of the previous tick. Weak so unloaded worlds drop out. */
    private final Set<World> wereAsleep = Collections.newSetFromMap(new WeakHashMap<World, Boolean>());

    @SubscribeEvent
    public void onWorldTick(TickEvent.WorldTickEvent event) {
        if (event.phase != TickEvent.Phase.START || event.side != Side.SERVER) return;

        World world = event.world;
        if (world.provider.hasNoSky) return;
        if (!Config.dimensions.contains(world.provider.dimensionId)) return;

        WorldInfo info = world.getWorldInfo();
        WeatherState state = getState(world);

        // Vanilla's rain timer is held high every tick, so 0 means something just reset the weather.
        // Sleeping and /weather clear reset it in exactly the same way. Sleeping is the one where every player
        // was asleep at the start of the previous tick (WorldServer wakes them and resets the weather in that
        // tick). That works whether or not doDaylightCycle moves the clock.
        boolean reset = info.getRainTime() <= 0;
        boolean sleptThrough = reset && wereAsleep.contains(world);
        if (world instanceof WorldServer && ((WorldServer) world).areAllPlayersAsleep()) wereAsleep.add(world);
        else wereAsleep.remove(world);
        info.setRainTime(HOLD_TICKS);
        info.setThunderTime(HOLD_TICKS);

        RainProfile profile = Config.byName(state.profile);
        if (profile == null) {
            // New world, or the saved profile was removed from the config: start at the bottom of the chain.
            profile = Config.profiles.get(0);
            startProfile(world, state, profile, 0, "start of chain");
        } else if (state.rain <= 0.2f) {
            // Saved by an older version without rolled strengths: roll the same profile again.
            startProfile(world, state, profile, 0, "re-rolled (old save)");
        } else if ((reset && !sleptThrough) || --state.ticksLeft <= 0) {
            // Profile ran out, or /weather clear: start the planned next profile. Sleeping changes nothing.
            RainProfile planned = Config.byName(state.nextProfile);
            int duration = state.nextTicks;
            if (planned == null || duration <= 0) {
                // Nothing planned (older save) or the planned profile was removed from the config: step now.
                planned = Config.profiles.get(nextIndex(Config.profiles.indexOf(profile)));
                duration = 0;
            }
            int from = Config.profiles.indexOf(profile), to = Config.profiles.indexOf(planned);
            String why = to == from ? "stayed on " + profile.name
                : (isForward(from, to) ? "forward from " : "back from ") + profile.name;
            profile = planned;
            startProfile(world, state, profile, duration, (reset ? "/weather clear, " : "") + why);
        } else if (Config.byName(state.nextProfile) == null || state.nextTicks <= 0) {
            // Older save without a plan, or the planned profile was removed from the config: plan it now.
            planNext(state, profile);
        }
        state.markDirty();

        info.setRaining(stepUp(world.rainingStrength, state.rain));
        info.setThundering(stepUp(world.thunderingStrength, state.thunder));

        sounds.tick(world, profile.sound);
    }

    /**
     * Starts a profile: rolls its strengths, uses the planned duration (or rolls one if 'duration' is 0), then
     * plans the profile after it.
     */
    private void startProfile(World world, WeatherState state, RainProfile profile, int duration, String why) {
        state.profile = profile.name;
        state.rain = profile.rollRain(rand);
        state.thunder = profile.rollThunder(rand);
        state.ticksLeft = duration > 0 ? duration : profile.rollDurationTicks(rand);
        planNext(state, profile);
        sounds.reset();
        Network.sendProfileToDimension(world.provider.dimensionId, profile); // per-profile rain sounds

        if (Config.debug) {
            RainyDay.LOG.info(
                "Starting {} (rain {}, thunder {}, for {} min) in dimension {} [{}], next: {} for {} min",
                profile.name,
                String.format("%.2f", state.rain),
                String.format("%.2f", state.thunder),
                String.format("%.1f", state.ticksLeft / 1200f),
                world.provider.dimensionId,
                why,
                state.nextProfile,
                String.format("%.1f", state.nextTicks / 1200f));
        }
    }

    /** Should vanilla push this strength up (true) or down (false) this tick to approach the target? */
    private static boolean stepUp(float current, float target) {
        if (target >= 1f) return true; // pinned at max, vanilla clamps it there
        if (target <= 0f) return false; // pinned at 0
        return current < target; // in between: settles within 0.01 of the target
    }

    /** Rolls the profile that will follow 'current' (back / stay / forward) and how long it will last. */
    private void planNext(WeatherState state, RainProfile current) {
        RainProfile next = Config.profiles.get(nextIndex(Config.profiles.indexOf(current)));
        state.nextProfile = next.name;
        state.nextTicks = next.rollDurationTicks(rand);
    }

    /**
     * One step along the chain from profile index 'from': back one, stay, or forward one.
     * The first profile can't go back, so its back chance is added to forward. Going forward from the last
     * profile wraps around to the first.
     */
    private int nextIndex(int from) {
        int count = Config.profiles.size();
        if (count <= 1) return 0;

        float back = Config.chanceBack, stay = Config.chanceStay, forward = Config.chanceForward;
        if (from == 0) {
            forward += back;
            back = 0f;
        }

        float roll = rand.nextFloat() * (back + stay + forward);
        if (roll < back) return from - 1;
        if (roll < back + stay) return from;
        return (from + 1) % count; // last wraps to first
    }

    /** True if going from index 'from' to 'to' was a forward step (including the wrap from last to first). */
    private static boolean isForward(int from, int to) {
        return to == (from + 1) % Config.profiles.size();
    }

    static WeatherState getState(World world) {
        WeatherState state = (WeatherState) world.perWorldStorage.loadData(WeatherState.class, WeatherState.NAME);
        if (state == null) {
            state = new WeatherState(WeatherState.NAME);
            world.perWorldStorage.setData(WeatherState.NAME, state);
        }
        return state;
    }
}
