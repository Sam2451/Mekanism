package mekanism.common.capabilities.fluid;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.IChemicalTank;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.common.capabilities.merged.MergedTank;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

/**
 * Like {@link mekanism.api.chemical.merged.ChemicalTankWrapper}
 */
@NothingNullByDefault
public class FluidTankWrapper implements IExtendedFluidTank {

    private final IChemicalTank<?, ?>[] chemicalTanks;
    private final IExtendedFluidTank internal;
    private final MergedTank mergedTank;

    public FluidTankWrapper(MergedTank mergedTank, IExtendedFluidTank internal, IChemicalTank<?, ?>... chemicalTanks) {
        //TODO: Do we want to short circuit it so that if we are not empty it allows for inserting before checking the insertCheck
        this.mergedTank = mergedTank;
        this.internal = internal;
        this.chemicalTanks = chemicalTanks;
    }

    public MergedTank getMergedTank() {
        return mergedTank;
    }

    @Override
    public void setStack(FluidStack stack) {
        internal.setStack(stack);
    }

    @Override
    public void setStackUnchecked(FluidStack stack) {
        internal.setStackUnchecked(stack);
    }

    private boolean canInsert() {
        for (IChemicalTank<?, ?> otherTank : chemicalTanks) {
            if (!otherTank.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public FluidStack insert(FluidStack stack, Action action, AutomationType automationType) {
        //Only allow inserting if we pass the check
        return canInsert() ? internal.insert(stack, action, automationType) : stack;
    }

    @Override
    public FluidStack extract(int amount, Action action, AutomationType automationType) {
        return internal.extract(amount, action, automationType);
    }

    @Override
    public void onContentsChanged() {
        internal.onContentsChanged();
    }

    @Override
    public int setStackSize(int amount, Action action) {
        return internal.setStackSize(amount, action);
    }

    @Override
    public int growStack(int amount, Action action) {
        return internal.growStack(amount, action);
    }

    @Override
    public int shrinkStack(int amount, Action action) {
        return internal.shrinkStack(amount, action);
    }

    @Override
    public boolean isEmpty() {
        return internal.isEmpty();
    }

    @Override
    public void setEmpty() {
        internal.setEmpty();
    }

    @Override
    public boolean isFluidEqual(FluidStack other) {
        return internal.isFluidEqual(other);
    }

    @Override
    public int getNeeded() {
        return internal.getNeeded();
    }

    @Override
    public CompoundTag serializeNBT(HolderLookup.Provider provider) {
        return internal.serializeNBT(provider);
    }

    @Override
    public boolean isCompatible(IExtendedFluidTank other) {
        if (getClass() == other.getClass()) {
            return internal.isCompatible(((FluidTankWrapper) other).internal);
        }
        return internal.isCompatible(other);
    }

    @Override
    public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
        internal.deserializeNBT(provider, nbt);
    }

    @NotNull
    @Override
    public FluidStack getFluid() {
        return internal.getFluid();
    }

    @Override
    public int getFluidAmount() {
        return internal.getFluidAmount();
    }

    @Override
    public int getCapacity() {
        return internal.getCapacity();
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return internal.isFluidValid(stack);
    }
}