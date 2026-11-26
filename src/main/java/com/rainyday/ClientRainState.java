package com.rainyday;

/**
 * The rain sound settings this player's game last received from the server.
 *
 * Kept free of client-only classes so it can load on a dedicated server too. Messages arrive on the network
 * thread and sounds play on the game thread, so the value is swapped as one object through a volatile field.
 */
public final class ClientRainState {

    private static volatile RainSound current = null; // null = not on a RainyDay server: vanilla rain
    private static volatile String profile = "";

    private ClientRainState() {}

    public static void set(String profileName, RainSound rain) {
        profile = profileName;
        current = profileName.isEmpty() ? null : rain;
        if (Config.debug) {
            RainyDay.LOG.info(
                "Rain sound: {}",
                profileName.isEmpty() ? "vanilla (dimension not managed)" : profileName + " -> " + rain);
        }
    }

    public static void clear() {
        profile = "";
        current = null;
    }

    public static RainSound get() {
        return current;
    }

    public static String profile() {
        return profile;
    }
}
