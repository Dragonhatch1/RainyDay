package com.rainyday;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraftforge.common.config.ConfigCategory;
import net.minecraftforge.common.config.Configuration;

public class Config {

    /** Dimension IDs RainyDay controls. 0 = Overworld. */
    public static Set<Integer> dimensions = new HashSet<>();

    /** Print "Starting <profile>" to the console whenever a profile begins. */
    public static boolean debug = false;

    /** Ambient sounds are multiplied by this when the player is under a roof. */
    public static float indoorSoundVolume = 0.4f;

    public static List<RainProfile> profiles = new ArrayList<>();

    /** Distant thunder: vanilla's thunder sound, quieter and deeper than a real strike. */
    private static final AmbientSound DISTANT_THUNDER = new AmbientSound(
        "ambient.weather.thunder",
        15f,
        40f,
        0.5f,
        0.5f,
        0.7f);

    // name, rain min/max, thunder min/max, minutes min/max, weight, ambient sound, rain splash sound
    // Rain splash sounds: event to play instead of vanilla rain, volume multiplier, share of splash sounds kept.
    private static final RainProfile[] DEFAULTS = {
        new RainProfile(
            "drizzle",
            0.3f,
            0.5f,
            0.0f,
            0.0f,
            15f,
            40f,
            3,
            null,
            new RainSound("rainyday:rain.drizzle", 1.0f, 0.6f)),
        new RainProfile(
            "steady",
            0.8f,
            1.0f,
            0.0f,
            0.1f,
            15f,
            40f,
            4,
            DISTANT_THUNDER,
            new RainSound("rainyday:rain.steady", 1.0f, 0.8f)),
        new RainProfile(
            "heavy",
            1.0f,
            1.0f,
            0.5f,
            0.7f,
            15f,
            40f,
            2,
            DISTANT_THUNDER,
            new RainSound("rainyday:rain.heavy", 1.2f, 1.0f)),
        new RainProfile(
            "storm",
            1.0f,
            1.0f,
            1.0f,
            1.0f,
            5f,
            15f,
            1,
            DISTANT_THUNDER,
            new RainSound("rainyday:rain.storm", 1.4f, 1.0f)), };

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        cfg.addCustomCategoryComment(
            "general",
            "RainyDay keeps it raining and randomly switches between the rain profiles listed below.\n"
                + "To add a profile, add its name to 'profiles' and restart: a section for it is created here.");

        debug = cfg.getBoolean(
            "debug",
            "general",
            false,
            "Print 'Starting <profile>' to the console (and log) each time a weather profile begins.");

        indoorSoundVolume = cfg.getFloat(
            "indoorSoundVolume",
            "general",
            indoorSoundVolume,
            0f,
            1f,
            "Profile sounds are multiplied by this when the player is under a roof (0 = silent indoors).");

        int[] dims = cfg
            .get(
                "general",
                "dimensions",
                new int[] { 0 },
                "Dimension IDs where it always rains. 0 = Overworld. Only list dimensions that have their own weather "
                    + "(most vanilla-style dimensions just copy the Overworld's weather).")
            .getIntList();
        dimensions.clear();
        for (int d : dims) dimensions.add(d);

        String[] defaultNames = new String[DEFAULTS.length];
        for (int i = 0; i < DEFAULTS.length; i++) defaultNames[i] = DEFAULTS[i].name;
        String[] names = cfg.get("general", "profiles", defaultNames, "Names of the rain profiles to use.")
            .getStringList();

        profiles.clear();
        for (String name : names) {
            RainProfile d = defaultFor(name);
            String cat = "profile_" + name;
            cfg.addCustomCategoryComment(
                cat,
                "Rain profile '" + name
                    + "'. Each time it starts, rain and thunder strength are picked at random "
                    + "between their min and max. Set min = max for a fixed value.");

            // Configs from an older version had single 'rain'/'thunder' values: use them as both min and max.
            float oldRain = takeOldValue(cfg, cat, "rain", -1f);
            float oldThunder = takeOldValue(cfg, cat, "thunder", -1f);
            float rainDefMin = oldRain >= 0 ? oldRain : d.rainMin, rainDefMax = oldRain >= 0 ? oldRain : d.rainMax;
            float thDefMin = oldThunder >= 0 ? oldThunder : d.thunderMin;
            float thDefMax = oldThunder >= 0 ? oldThunder : d.thunderMax;

            String rainHelp = "1 = normal vanilla rain, lower = fainter. Must stay above 0.2 to count as rain.";
            float rainMin = cfg.getFloat("rainMin", cat, rainDefMin, 0.25f, 1f, "Lowest rain strength. " + rainHelp);
            float rainMax = cfg.getFloat("rainMax", cat, rainDefMax, 0.25f, 1f, "Highest rain strength.");
            String thunderHelp = "Darkens the sky. Lightning (and daytime mob spawning) only happens when "
                + "rain x thunder is above 0.9.";
            float thMin = cfg.getFloat("thunderMin", cat, thDefMin, 0f, 1f, "Lowest thunder strength. " + thunderHelp);
            float thMax = cfg.getFloat("thunderMax", cat, thDefMax, 0f, 1f, "Highest thunder strength.");
            float min = cfg.getFloat("minMinutes", cat, d.minMinutes, 0.05f, 100000f, "Shortest this profile lasts.");
            float max = cfg.getFloat("maxMinutes", cat, d.maxMinutes, 0.05f, 100000f, "Longest this profile lasts.");
            int weight = cfg.getInt(
                "weight",
                cat,
                d.weight,
                0,
                10000,
                "How likely this profile is to be picked, relative to the others. 0 = never.");

            profiles.add(
                new RainProfile(
                    name,
                    rainMin,
                    rainMax,
                    thMin,
                    thMax,
                    min,
                    max,
                    weight,
                    loadSound(cfg, cat, d),
                    loadRainSound(cfg, cat, d)));
        }

        int total = 0;
        for (RainProfile p : profiles) total += p.weight;
        if (total <= 0) {
            RainyDay.LOG.warn("No rain profiles with weight above 0; using plain steady rain.");
            profiles.clear();
            profiles.add(new RainProfile("steady", 1f, 1f, 0f, 0f, 10f, 40f, 1, null, RainSound.VANILLA));
        }

        if (cfg.hasChanged()) cfg.save();
    }

    private static AmbientSound loadSound(Configuration cfg, String cat, RainProfile d) {
        // Profiles without a default sound still get the settings, with an empty name, so one is easy to add.
        AmbientSound s = d.sound != null ? d.sound : new AmbientSound("", 15f, 40f, 0.5f, 0.5f, 0.7f);

        String event = cfg.getString(
            "sound",
            cat,
            s.event,
            "Sound played near each player every so often during this profile. Empty = no sound. "
                + "e.g. ambient.weather.thunder (vanilla thunder). Players' resource packs decide which file plays.");
        float minSec = cfg
            .getFloat("soundMinSeconds", cat, s.minSeconds, 1f, 3600f, "Shortest gap between sounds, in seconds.");
        float maxSec = cfg
            .getFloat("soundMaxSeconds", cat, s.maxSeconds, 1f, 3600f, "Longest gap between sounds, in seconds.");
        float volume = cfg.getFloat(
            "soundVolume",
            cat,
            s.volume,
            0.01f,
            1f,
            "Roughly how loud the sound is to the player, 0-1. Real lightning strikes play at full volume.");
        float pitchMin = cfg.getFloat(
            "soundPitchMin",
            cat,
            s.pitchMin,
            0.5f,
            2f,
            "Lowest pitch. Lower = deeper and more distant-sounding. Players' games limit pitch to 0.5-2.");
        float pitchMax = cfg.getFloat("soundPitchMax", cat, s.pitchMax, 0.5f, 2f, "Highest pitch.");

        event = event.trim();
        if (event.isEmpty()) return null;
        return new AmbientSound(event, minSec, maxSec, volume, pitchMin, pitchMax);
    }

    private static RainSound loadRainSound(Configuration cfg, String cat, RainProfile d) {
        RainSound r = d.rainSound;
        String sound = cfg.getString(
            "rainSound",
            cat,
            r.sound,
            "Plays instead of vanilla's rain splash sound during this profile, for players who have RainyDay "
                + "installed (others hear normal rain). Empty = vanilla rain. e.g. rainyday:rain.heavy. "
                + "If the sound isn't defined in any sounds.json, players hear vanilla rain.");
        float volume = cfg.getFloat(
            "rainSoundVolume",
            cat,
            r.volume,
            0f,
            5f,
            "Multiplies the rain splash volume (vanilla plays it at 0.2, or 0.1 under a roof). 1 = unchanged.");
        float chance = cfg.getFloat(
            "rainSoundChance",
            cat,
            r.chance,
            0f,
            1f,
            "Share of rain splash sounds that play, 0-1. Vanilla plays about 7 a second; 0.5 = about half as many, "
                + "for a lighter, sparser sound. 0 = silent rain.");
        return new RainSound(sound, volume, chance);
    }

    /** Reads and removes a key left over from an older config version. Returns fallback if absent. */
    private static float takeOldValue(Configuration cfg, String cat, String key, float fallback) {
        ConfigCategory category = cfg.getCategory(cat);
        if (!category.containsKey(key)) return fallback;
        float value = (float) category.get(key)
            .getDouble();
        category.remove(key);
        return value;
    }

    private static RainProfile defaultFor(String name) {
        for (RainProfile p : DEFAULTS) if (p.name.equals(name)) return p;
        return new RainProfile(name, 1.0f, 1.0f, 0.0f, 0.0f, 10f, 30f, 1, null, RainSound.VANILLA);
    }

    public static RainProfile byName(String name) {
        for (RainProfile p : profiles) if (p.name.equals(name)) return p;
        return null;
    }
}
