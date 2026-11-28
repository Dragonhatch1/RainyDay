package com.rainyday;

import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.client.renderer.texture.IIconRegister;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * Weather TV: right-click it to get the forecast in chat (current profile + time left, and the next profile +
 * how long it will last). Faces the player who placed it. Metadata = the side the screen is on (2-5, like a
 * furnace).
 */
public class BlockWeatherTV extends Block {

    @SideOnly(Side.CLIENT)
    private IIcon front, side, top;

    public BlockWeatherTV() {
        super(Material.iron);
        setBlockName("rainyday.weather_tv");
        setHardness(2.0F);
        setResistance(10.0F);
        setStepSound(soundTypeMetal);
        setCreativeTab(CreativeTabs.tabDecorations);
    }

    @Override
    public boolean onBlockActivated(World world, int x, int y, int z, EntityPlayer player, int hitSide, float hitX,
        float hitY, float hitZ) {
        if (!world.isRemote) Forecast.sendTo(player, world);
        return true; // also on the client, so the hand swings and nothing else happens
    }

    /** Turn the screen toward the player who placed it. */
    @Override
    public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase placer, ItemStack stack) {
        int facing = MathHelper.floor_double(placer.rotationYaw * 4.0F / 360.0F + 0.5D) & 3;
        int[] screenSide = { 2, 5, 3, 4 }; // player facing south, west, north, east -> screen north, east, south, west
        world.setBlockMetadataWithNotify(x, y, z, screenSide[facing], 2);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void registerBlockIcons(IIconRegister reg) {
        front = reg.registerIcon(RainyDay.MODID + ":weather_tv_front");
        side = reg.registerIcon(RainyDay.MODID + ":weather_tv_side");
        top = reg.registerIcon(RainyDay.MODID + ":weather_tv_top");
    }

    @Override
    @SideOnly(Side.CLIENT)
    public IIcon getIcon(int s, int meta) {
        if (s == 0 || s == 1) return top;
        int screen = meta < 2 || meta > 5 ? 3 : meta; // meta 0 = held in hand / inventory: screen faces you (south)
        return s == screen ? front : side;
    }
}
