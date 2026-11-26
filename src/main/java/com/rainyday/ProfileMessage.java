package com.rainyday;

import cpw.mods.fml.common.network.ByteBufUtils;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * Server -> player: "this is the current profile and how its rain should sound".
 * Sent when a profile starts, and when a player joins, respawns or changes dimension.
 *
 * The server's config is the one that counts, so players never need their own settings.
 * Players without RainyDay don't have this channel, and their game quietly ignores the message.
 */
public class ProfileMessage implements IMessage {

    /** Profile name, or "" when the player's dimension isn't managed by RainyDay (vanilla rain). */
    public String profile = "";
    public String sound = "";
    public float volume = 1f;
    public float chance = 1f;

    /** Needed by Forge to create the message when it arrives. */
    public ProfileMessage() {}

    public ProfileMessage(String profile, RainSound rain) {
        this.profile = profile == null ? "" : profile;
        RainSound r = rain == null ? RainSound.VANILLA : rain;
        this.sound = r.sound;
        this.volume = r.volume;
        this.chance = r.chance;
    }

    @Override
    public void toBytes(ByteBuf buf) {
        ByteBufUtils.writeUTF8String(buf, profile);
        ByteBufUtils.writeUTF8String(buf, sound);
        buf.writeFloat(volume);
        buf.writeFloat(chance);
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        profile = ByteBufUtils.readUTF8String(buf);
        sound = ByteBufUtils.readUTF8String(buf);
        volume = buf.readFloat();
        chance = buf.readFloat();
    }

    /** Runs on the player's game. Only stores the values; the sound swap happens in RainSoundHandler. */
    public static class Handler implements IMessageHandler<ProfileMessage, IMessage> {

        @Override
        public IMessage onMessage(ProfileMessage msg, MessageContext ctx) {
            ClientRainState.set(msg.profile, new RainSound(msg.sound, msg.volume, msg.chance));
            return null;
        }
    }
}
