package com.rainyday;

import java.util.Map;
import java.util.Random;
import java.util.WeakHashMap;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.S29PacketSoundEffect;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

/**
 * Plays a profile's ambient sound to each player on their own random timer.
 *
 * The sound is sent straight to each player (the same packet vanilla uses for any sound), placed a few
 * blocks away in a random direction so it pans left/right. Players' games pick which file to play from
 * their resource packs, so this needs nothing installed on their side.
 */
public class AmbientSoundPlayer {

    // In 1.7.10 a sound at volume <= 1 fades out linearly over 16 blocks.
    private static final float FADE_DISTANCE = 16f;
    private static final float MIN_OFFSET = 2f, MAX_OFFSET = 5f;

    private final Random rand = new Random();
    /** Ticks until each player's next sound. Weak keys so players who leave are forgotten. */
    private final Map<EntityPlayer, Integer> countdown = new WeakHashMap<>();

    /** Call when a new profile starts so everyone gets a fresh random delay for the new sound. */
    public void reset() {
        countdown.clear();
    }

    public void tick(World world, AmbientSound sound) {
        if (sound == null) return;
        // Wait for the rain to fade in before rumbling.
        if (!world.isRaining()) return;

        for (EntityPlayer player : world.playerEntities) {
            if (!(player instanceof EntityPlayerMP)) continue;

            Integer left = countdown.get(player);
            if (left == null) left = sound.rollDelayTicks(rand); // new player or new profile
            if (--left <= 0) {
                play((EntityPlayerMP) player, world, sound);
                left = sound.rollDelayTicks(rand);
            }
            countdown.put(player, left);
        }
    }

    private void play(EntityPlayerMP player, World world, AmbientSound sound) {
        double angle = rand.nextDouble() * Math.PI * 2;
        float offset = MIN_OFFSET + rand.nextFloat() * (MAX_OFFSET - MIN_OFFSET);
        double x = player.posX + Math.cos(angle) * offset;
        double y = player.posY + 3;
        double z = player.posZ + Math.sin(angle) * offset;

        // Make up for the fade over 'offset' blocks so the player hears roughly 'volume'.
        float volume = sound.volume / (1f - offset / FADE_DISTANCE);

        // Under a roof: quieter, like vanilla rain.
        int px = MathHelper.floor_double(player.posX), pz = MathHelper.floor_double(player.posZ);
        if (world.getPrecipitationHeight(px, pz) > MathHelper.floor_double(player.posY)) {
            volume *= Config.indoorSoundVolume;
        }

        volume = Math.min(volume, 1f); // above 1 would only stretch the range, not get louder
        if (volume <= 0f) return;

        player.playerNetServerHandler
            .sendPacket(new S29PacketSoundEffect(sound.event, x, y, z, volume, sound.rollPitch(rand)));
    }
}
