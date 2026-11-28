package com.rainyday;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;

/**
 * Saved with each world (data/rainyday.dat): the current profile, the rain and thunder strength rolled
 * for it, and how long it has left.
 */
public class WeatherState extends WorldSavedData {

    public static final String NAME = RainyDay.MODID;

    public String profile = "";
    public float rain = 0f;
    public float thunder = 0f;
    public int ticksLeft = 0;
    /** The profile planned to come next (rolled when the current one started) and how long it will last. */
    public String nextProfile = "";
    public int nextTicks = 0;

    // Forge creates this by reflection when loading, so it needs this exact constructor.
    public WeatherState(String name) {
        super(name);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        profile = tag.getString("profile");
        rain = tag.getFloat("rain"); // 0 if saved by an older version: that triggers a fresh roll
        thunder = tag.getFloat("thunder");
        ticksLeft = tag.getInteger("ticksLeft");
        nextProfile = tag.getString("nextProfile"); // "" if saved by an older version: planned on the next tick
        nextTicks = tag.getInteger("nextTicks");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("profile", profile);
        tag.setFloat("rain", rain);
        tag.setFloat("thunder", thunder);
        tag.setInteger("ticksLeft", ticksLeft);
        tag.setString("nextProfile", nextProfile);
        tag.setInteger("nextTicks", nextTicks);
    }
}
