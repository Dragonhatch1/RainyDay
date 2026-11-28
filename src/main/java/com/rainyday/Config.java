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

    // HUD weather icon (players' own setting, saved by the in-game placement screen).
    public static final String[] HUD_ANCHORS = { "top_left", "top_right", "bottom_left", "bottom_right" };
    /** Corner the offsets are measured from: the corner nearest the icon when it was last placed. */
    public static String hudAnchor = "top_left";
    /** Distance from that corner to the icon's nearest edges, in GUI pixels. */
    public static int hudX = 4, hudY = 4;
    /** Icon size: 1.0 = 16 GUI pixels (about an item icon). */
    public static float hudScale = 1.0f;
    public static final float HUD_SCALE_MIN = 0.5f, HUD_SCALE_MAX = 4.0f;

    /** Kept so the placement screen can save while the game runs. */
    private static Configuration config;

    /** In chain order: the order of the 'profiles' list. */
    public static List<RainProfile> profiles = new ArrayList<>();

    /**
     * Weather chain: when a profile ends, the chance to go back one, stay (a fresh roll of the same profile) or
     * go forward one. The first profile can't go back (its share goes forward); the last one wraps forward to
     * the first. Normalised to add up to 1.
     */
    public static float chanceBack = 0.20f, chanceStay = 0.35f, chanceForward = 0.45f;

    /** Distant thunder: vanilla's thunder sound, quieter and deeper than a real strike. */
    private static final AmbientSound DISTANT_THUNDER = new AmbientSound(
        "ambient.weather.thunder",
        15f,
        40f,
        0.5f,
        0.5f,
        0.7f);

    // name, rain min/max, thunder min/max, minutes min/max, ambient sound, rain splash sound
    // The order here is the default order of the weather chain: drizzle -> steady -> heavy -> storm.
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
            null,
            new RainSound("rainyday:rain.drizzle", 1.0f, 0.8f)),
        new RainProfile(
            "steady",
            0.8f,
            1.0f,
            0.2f,
            0.3f,
            15f,
            40f,
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
            DISTANT_THUNDER,
            new RainSound("rainyday:rain.storm", 1.4f, 1.0f)), };

    public static void load(File file) {
        Configuration cfg = new Configuration(file);
        config = cfg;
        cfg.addCustomCategoryComment(
            "general",
            "RainyDay keeps it raining and moves through the rain profiles listed below as a chain, in the order of\n"
                + "'profiles'. When a profile ends it stays, goes back one or goes forward one (see the chance settings).\n"
                + "To add a profile, add its name to 'profiles' and restart: a section for it is created here.");

        chanceBack = cfg.getFloat(
            "chanceBack",
            "general",
            chanceBack,
            0f,
            1f,
            "When a profile ends: chance to go back to the previous profile in the chain. "
                + "The first profile can't go back; this share goes to 'forward' instead.");
        chanceStay = cfg.getFloat(
            "chanceStay",
            "general",
            chanceStay,
            0f,
            1f,
            "When a profile ends: chance to stay on it (a fresh roll of strengths and duration).");
        chanceForward = cfg.getFloat(
            "chanceForward",
            "general",
            chanceForward,
            0f,
            1f,
            "When a profile ends: chance to go forward to the next profile. The last profile wraps to the first. "
                + "The three chances are scaled to add up to 1 if they don't already.");
        float sum = chanceBack + chanceStay + chanceForward;
        if (sum <= 0f) {
            RainyDay.LOG.warn("chanceBack + chanceStay + chanceForward is 0; profiles will always stay.");
            chanceBack = 0f;
            chanceStay = 1f;
            chanceForward = 0f;
        } else {
            chanceBack /= sum;
            chanceStay /= sum;
            chanceForward /= sum;
        }

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
        String[] names = cfg
            .get(
                "general",
                "profiles",
                defaultNames,
                "Names of the rain profiles to use, in chain order. The first can't go back; the last wraps forward "
                    + "to the first. A new world starts at the first.")
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
            // 'weight' is no longer used (the chain replaced weighted random picks): drop it from old configs.
            if (cfg.getCategory(cat)
                .containsKey("weight")) {
                cfg.getCategory(cat)
                    .remove("weight");
            }

            profiles.add(
                new RainProfile(
                    name,
                    rainMin,
                    rainMax,
                    thMin,
                    thMax,
                    min,
                    max,
                    loadSound(cfg, cat, d),
                    loadRainSound(cfg, cat, d)));
        }

        if (profiles.isEmpty()) {
            RainyDay.LOG.warn("No rain profiles listed; using plain steady rain.");
            profiles.add(new RainProfile("steady", 1f, 1f, 0f, 0f, 10f, 40f, null, RainSound.VANILLA));
        }

        loadHud(cfg);

        if (cfg.hasChanged()) cfg.save();
    }

    private static void loadHud(Configuration cfg) {
        cfg.addCustomCategoryComment(
            "hud",
            "Weather icon on your screen (only for players who have RainyDay installed). Easiest to change in game: "
                + "press the RainyDay HUD key (default H) and drag the icon; use the mouse wheel to resize it.");
        hudAnchor = cfg.getString(
            "anchor",
            "hud",
            hudAnchor,
            "Corner the offsets are measured from: top_left, top_right, bottom_left or bottom_right.",
            HUD_ANCHORS);
        boolean known = false;
        for (String a : HUD_ANCHORS) known |= a.equals(hudAnchor);
        if (!known) hudAnchor = "top_left";
        hudX = cfg.getInt("offsetX", "hud", hudX, 0, 10000, "Distance from that corner, sideways, in GUI pixels.");
        hudY = cfg.getInt("offsetY", "hud", hudY, 0, 10000, "Distance from that corner, up or down, in GUI pixels.");
        hudScale = cfg.getFloat(
            "scale",
            "hud",
            hudScale,
            HUD_SCALE_MIN,
            HUD_SCALE_MAX,
            "Icon size. 1.0 = 16 GUI pixels; 0.5 to 4.0.");
    }

    /** Saves the HUD icon position and size (called by the placement screen). */
    public static void saveHud() {
        if (config == null) return;
        config.getCategory("hud")
            .get("anchor")
            .set(hudAnchor);
        config.getCategory("hud")
            .get("offsetX")
            .set(hudX);
        config.getCategory("hud")
            .get("offsetY")
            .set(hudY);
        config.getCategory("hud")
            .get("scale")
            .set(hudScale);
        config.save();
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
        return new RainProfile(name, 1.0f, 1.0f, 0.0f, 0.0f, 10f, 30f, null, RainSound.VANILLA);
    }

    public static RainProfile byName(String name) {
        for (RainProfile p : profiles) if (p.name.equals(name)) return p;
        return null;
    }
}
