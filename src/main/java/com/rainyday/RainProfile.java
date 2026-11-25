package com.rainyday;

import java.util.Random;

/**
 * One kind of weather. When it starts, its actual rain and thunder strength are rolled at random
 * between the min and max given here, along with how long it lasts.
 */
public class RainProfile {

    private static final int TICKS_PER_MINUTE = 20 * 60;

    public final String name;
    /** Rain strength range, 0.25-1. Above 0.2 counts as raining. Lower = fainter rain. */
    public final float rainMin, rainMax;
    /** Thunder strength range, 0-1. Darkens the sky. Lightning only when rain x thunder is above 0.9. */
    public final float thunderMin, thunderMax;
    public final float minMinutes, maxMinutes;
    /** Relative chance of being picked. 0 disables the profile. */
    public final int weight;
    /** Optional sound played near each player every so often while this profile is active. Null = none. */
    public final AmbientSound sound;

    public RainProfile(String name, float rainMin, float rainMax, float thunderMin, float thunderMax, float minMinutes,
        float maxMinutes, int weight, AmbientSound sound) {
        this.name = name;
        this.rainMin = Math.min(rainMin, rainMax);
        this.rainMax = Math.max(rainMin, rainMax);
        this.thunderMin = Math.min(thunderMin, thunderMax);
        this.thunderMax = Math.max(thunderMin, thunderMax);
        this.minMinutes = Math.min(minMinutes, maxMinutes);
        this.maxMinutes = Math.max(minMinutes, maxMinutes);
        this.weight = weight;
        this.sound = sound;
    }

    public float rollRain(Random rand) {
        return rollBetween(rand, rainMin, rainMax);
    }

    public float rollThunder(Random rand) {
        return rollBetween(rand, thunderMin, thunderMax);
    }

    public int rollDurationTicks(Random rand) {
        int min = Math.max(1, Math.round(minMinutes * TICKS_PER_MINUTE));
        int max = Math.max(min, Math.round(maxMinutes * TICKS_PER_MINUTE));
        return min + rand.nextInt(max - min + 1);
    }

    private static float rollBetween(Random rand, float min, float max) {
        return min + rand.nextFloat() * (max - min);
    }

    @Override
    public String toString() {
        return String.format(
            "%s (rain %.2f-%.2f, thunder %.2f-%.2f, %.1f-%.1f min, weight %d, sound: %s)",
            name,
            rainMin,
            rainMax,
            thunderMin,
            thunderMax,
            minMinutes,
            maxMinutes,
            weight,
            sound == null ? "none" : sound);
    }
}
