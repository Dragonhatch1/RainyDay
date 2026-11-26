package com.rainyday;

/**
 * How a profile changes vanilla's rain splash sound (ambient.weather.rain) on players' games.
 * Only players who have RainyDay installed hear the difference; everyone else hears vanilla rain.
 */
public class RainSound {

    /** Plain vanilla rain: no swap, no volume change, nothing skipped. */
    public static final RainSound VANILLA = new RainSound("", 1f, 1f);

    /** Sound event to play instead, e.g. "rainyday:rain.heavy". Empty = keep vanilla's rain sound. */
    public final String sound;
    /** Multiplies the rain sound's volume (vanilla plays it at 0.2, or 0.1 under a roof). */
    public final float volume;
    /** Share of rain sounds that play, 0-1. Lower = sparser, gentler rain. */
    public final float chance;

    public RainSound(String sound, float volume, float chance) {
        this.sound = sound == null ? "" : sound.trim();
        this.volume = volume;
        this.chance = chance;
    }

    public boolean isVanilla() {
        return sound.isEmpty() && volume == 1f && chance >= 1f;
    }

    @Override
    public String toString() {
        return String.format(
            "%s, volume x%.2f, %d%% of sounds",
            sound.isEmpty() ? "vanilla rain" : sound,
            volume,
            Math.round(chance * 100));
    }
}
