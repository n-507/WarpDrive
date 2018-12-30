package com.raoulvdberge.refinedstorage.tile;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;

public abstract class TileBase extends TileEntity {

    public void setDirection(final EnumFacing direction) { }

    public EnumFacing getDirection() {
        return null;
    }
}
