package net.umf.polyfactory.block.entity;

import net.neoforged.neoforge.transfer.fluid.FluidStacksResourceHandler;

/**
 * Single-tank fluid buffer for the Fabricator: one slot, any fluid, filled/drained via buckets
 * (see {@link net.umf.polyfactory.block.FabricatorBlock}) or piped in/out from any side. Fluid
 * Upgrades need to raise the capacity at runtime, so {@link #setCapacity} exposes the protected
 * field {@link FluidStacksResourceHandler} otherwise only sets at construction time.
 */
public class FabricatorFluidHandler extends FluidStacksResourceHandler {

    public FabricatorFluidHandler(int capacity) {
        super(1, capacity);
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }
}
