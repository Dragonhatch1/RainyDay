package com.rainyday;

import java.util.Random;

/** A sound a profile plays near each player every so often, e.g. distant thunder during heavy rain. */
public class AmbientSound {

    /** Sound event name, e.g. "ambient.weather.thunder". Players' resource packs decide which file plays. */
    public final String event;
    public final float minSeconds, maxSeconds;
    /** Roughly how loud it sounds to the player, 0-1. */
    public final float volume;
    /** Players' games clamp pitch to 0.5-2. Lower = deeper and more distant-sounding. */
    public final float pitchMin, pitchMax;

    public AmbientSound(String event, float minSeconds, float maxSeconds, float volume, float pitchMin,
        float pitchMax) {
        this.event = event;
        this.minSeconds = Math.min(minSeconds, maxSeconds);
        this.maxSeconds = Math.max(minSeconds, maxSeconds);
        this.volume = volume;
        this.pitchMin = Math.min(pitchMin, pitchMax);
        this.pitchMax = Math.max(pitchMin, pitchMax);
    }

    public int rollDelayTicks(Random rand) {
        int min = Math.max(1, Math.round(minSeconds * 20));
        int max = Math.max(min, Math.round(maxSeconds * 20));
        return min + rand.nextInt(max - min + 1);
    }

    public float rollPitch(Random rand) {
        return pitchMin + rand.nextFloat() * (pitchMax - pitchMin);
    }

    @Override
    public String toString() {
        return String.format(
            "%s every %.0f-%.0f s, volume %.2f, pitch %.2f-%.2f",
            event,
            minSeconds,
            maxSeconds,
            volume,
            pitchMin,
            pitchMax);
    }
}
