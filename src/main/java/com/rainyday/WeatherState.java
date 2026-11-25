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
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("profile", profile);
        tag.setFloat("rain", rain);
        tag.setFloat("thunder", thunder);
        tag.setInteger("ticksLeft", ticksLeft);
    }
}
