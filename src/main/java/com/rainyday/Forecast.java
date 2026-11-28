package com.rainyday;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;

/** Server side: builds the Weather TV's forecast and sends it to one player's chat (only they see it). */
public final class Forecast {

    private static final String PREFIX = EnumChatFormatting.AQUA + "[Weather TV] " + EnumChatFormatting.RESET;

    private Forecast() {}

    public static void sendTo(EntityPlayer player, World world) {
        if (world.provider.hasNoSky || !Config.dimensions.contains(world.provider.dimensionId)) {
            say(player, EnumChatFormatting.GRAY + "No weather forecast here.");
            return;
        }

        WeatherState state = WeatherHandler.getState(world);
        RainProfile now = Config.byName(state.profile);
        if (now == null) {
            say(player, EnumChatFormatting.GRAY + "The forecast isn't ready yet. Try again in a moment.");
            return;
        }

        say(
            player,
            "Now: " + EnumChatFormatting.WHITE
                + displayName(now.name)
                + EnumChatFormatting.GRAY
                + " for another "
                + EnumChatFormatting.WHITE
                + duration(state.ticksLeft));

        RainProfile next = Config.byName(state.nextProfile);
        if (next != null && state.nextTicks > 0) {
            boolean same = next.name.equals(now.name);
            say(
                player,
                "Next: " + EnumChatFormatting.WHITE
                    + displayName(next.name)
                    + (same ? " again" : "")
                    + EnumChatFormatting.GRAY
                    + ", lasting "
                    + EnumChatFormatting.WHITE
                    + duration(state.nextTicks));
        }
    }

    private static void say(EntityPlayer player, String text) {
        player.addChatMessage(new ChatComponentText(PREFIX + EnumChatFormatting.GRAY + text));
    }

    /** "heavy" -> "Heavy". */
    static String displayName(String profile) {
        if (profile.isEmpty()) return profile;
        return Character.toUpperCase(profile.charAt(0)) + profile.substring(1);
    }

    /** Ticks as real time: "45s", "12m 30s", "1h 05m". */
    static String duration(int ticks) {
        int seconds = Math.max(0, ticks) / 20;
        int h = seconds / 3600, m = (seconds % 3600) / 60, s = seconds % 60;
        if (h > 0) return String.format("%dh %02dm", h, m);
        if (m > 0) return String.format("%dm %02ds", m, s);
        return s + "s";
    }
}
