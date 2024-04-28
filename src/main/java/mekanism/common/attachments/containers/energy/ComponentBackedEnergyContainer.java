package mekanism.common.attachments.containers.energy;

import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.NBTConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.common.attachments.containers.ComponentBackedContainer;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class ComponentBackedEnergyContainer extends ComponentBackedContainer<FloatingLong, AttachedEnergy> implements IEnergyContainer {

    private final Predicate<@NotNull AutomationType> canExtract;
    private final Predicate<@NotNull AutomationType> canInsert;
    private final FloatingLongSupplier maxEnergy;
    private final FloatingLongSupplier rate;

    public ComponentBackedEnergyContainer(ItemStack attachedTo, int containerIndex, Predicate<@NotNull AutomationType> canExtract,
          Predicate<@NotNull AutomationType> canInsert, FloatingLongSupplier rate, FloatingLongSupplier maxEnergy) {
        super(attachedTo, containerIndex);
        this.canExtract = canExtract;
        this.canInsert = canInsert;
        this.maxEnergy = maxEnergy;
        this.rate = rate;
    }

    @Override
    protected Supplier<? extends DataComponentType<AttachedEnergy>> dataComponentType() {
        return MekanismDataComponents.ATTACHED_ENERGY;
    }

    @Override
    protected FloatingLong copy(FloatingLong toCopy) {
        return toCopy.copyAsConst();
    }

    @Override
    protected boolean isEmpty(FloatingLong value) {
        return value.isZero();
    }

    @Override
    public FloatingLong getEnergy() {
        //TODO - 1.20.5: Similar to getBasicInventorySlot do we want to reduce calls to this? Probably (We mostly do so, but we probably want to add a note here)
        AttachedEnergy attachedEnergy = getAttached();
        return attachedEnergy == null ? FloatingLong.ZERO : getContents(attachedEnergy);
    }

    @Override
    public void setEnergy(FloatingLong energy) {
        setContents(energy);
    }

    protected FloatingLong clampEnergy(FloatingLong energy) {
        return energy.min(getMaxEnergy());
    }

    @Override
    protected void setContents(AttachedEnergy attachedEnergy, FloatingLong energy) {
        super.setContents(attachedEnergy, clampEnergy(energy));
    }

    protected FloatingLong getRate(@Nullable AutomationType automationType) {
        //Allow unknown or manual interaction to bypass rate limit for the item
        return automationType == null || automationType == AutomationType.MANUAL ? FloatingLong.MAX_VALUE : rate.get();
    }

    @Override
    public FloatingLong insert(FloatingLong amount, Action action, AutomationType automationType) {
        if (amount.isZero() || !canInsert.test(automationType)) {
            return amount;
        }
        AttachedEnergy attachedEnergy = getAttached();
        if (attachedEnergy == null) {
            return amount;
        }
        FloatingLong stored = getContents(attachedEnergy);
        FloatingLong needed = getRate(automationType).min(getNeeded(stored));
        if (needed.isZero()) {
            //Fail if we are a full container or our rate is zero
            return amount;
        }
        FloatingLong toAdd = amount.min(needed);
        if (!toAdd.isZero() && action.execute()) {
            //If we want to actually insert the energy, then update the current energy
            // Note: this also will mark that the contents changed
            setContents(attachedEnergy, stored.add(toAdd));
        }
        return amount.subtract(toAdd);
    }

    @Override
    public FloatingLong extract(FloatingLong amount, Action action, AutomationType automationType) {
        if (amount.isZero()) {
            return FloatingLong.ZERO;
        }
        AttachedEnergy attachedEnergy = getAttached();
        if (attachedEnergy == null) {
            return FloatingLong.ZERO;
        }
        FloatingLong stored = getContents(attachedEnergy);
        if (stored.isZero() || !canExtract.test(automationType)) {
            return FloatingLong.ZERO;
        }
        FloatingLong ret = getRate(automationType).min(stored).min(amount).copy();
        if (!ret.isZero() && action.execute()) {
            //Note: this also will mark that the contents changed
            setContents(attachedEnergy, stored.subtract(ret));
        }
        return ret;
    }

    protected FloatingLong getNeeded(FloatingLong stored) {
        return getMaxEnergy().subtract(stored);
    }

    @Override
    public FloatingLong getMaxEnergy() {
        return maxEnergy.get();
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        //TODO - 1.20.5: This is a copy of BasicEnergyContainer#serializeNBT. We might need to also grab the specific overrides of
        // that method as special component backed inventory slots, that then access and put that other data as a different component?
        CompoundTag nbt = new CompoundTag();
        FloatingLong stored = getEnergy();
        if (!stored.isZero()) {
            nbt.putString(NBTConstants.STORED, stored.toString());
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        NBTUtils.setFloatingLongIfPresent(nbt, NBTConstants.STORED, this::setEnergy);
    }
}