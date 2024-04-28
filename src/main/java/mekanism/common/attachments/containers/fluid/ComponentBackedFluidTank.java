package mekanism.common.attachments.containers.fluid;

import java.util.function.BiPredicate;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.NBTConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.fluid.IExtendedFluidTank;
import mekanism.common.attachments.containers.ComponentBackedContainer;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.util.NBTUtils;
import net.minecraft.core.HolderLookup.Provider;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public class ComponentBackedFluidTank extends ComponentBackedContainer<FluidStack, AttachedFluids> implements IExtendedFluidTank {

    private final BiPredicate<@NotNull FluidStack, @NotNull AutomationType> canExtract;
    private final BiPredicate<@NotNull FluidStack, @NotNull AutomationType> canInsert;
    private final Predicate<@NotNull FluidStack> validator;
    private final IntSupplier capacity;
    private final IntSupplier rate;

    public ComponentBackedFluidTank(ItemStack attachedTo, int tankIndex, BiPredicate<@NotNull FluidStack, @NotNull AutomationType> canExtract,
          BiPredicate<@NotNull FluidStack, @NotNull AutomationType> canInsert, Predicate<@NotNull FluidStack> validator, IntSupplier rate, IntSupplier capacity) {
        super(attachedTo, tankIndex);
        this.canExtract = canExtract;
        this.canInsert = canInsert;
        this.validator = validator;
        this.capacity = capacity;
        this.rate = rate;
    }

    @Override
    protected Supplier<? extends DataComponentType<AttachedFluids>> dataComponentType() {
        return MekanismDataComponents.ATTACHED_FLUIDS;
    }

    @Override
    protected FluidStack copy(FluidStack toCopy) {
        return toCopy.copy();
    }

    @Override
    protected boolean isEmpty(FluidStack value) {
        return value.isEmpty();
    }

    @Override
    public FluidStack getFluid() {
        //TODO - 1.20.5: Similar to getBasicInventorySlot do we want to reduce calls to this? Probably (We mostly do so, but we probably want to add a note here)
        AttachedFluids attachedFluids = getAttached();
        return attachedFluids == null ? FluidStack.EMPTY : getContents(attachedFluids);
    }

    @Override
    public void setStack(FluidStack stack) {
        //TODO - 1.20.5: I think just always setting it as unchecked is fine as we are doing that for items regardless
        // but we may want to validate it?
        setStackUnchecked(stack);
    }

    @Override
    public void setStackUnchecked(FluidStack stack) {
        setContents(stack);
    }

    @Override
    public int getCapacity() {
        return capacity.getAsInt();
    }

    protected int getRate(@Nullable AutomationType automationType) {
        //Allow unknown or manual interaction to bypass rate limit for the item
        return automationType == null || automationType == AutomationType.MANUAL ? Integer.MAX_VALUE : rate.getAsInt();
    }

    @Override
    public FluidStack insert(FluidStack stack, Action action, AutomationType automationType) {
        //TODO - 1.20.5: Items do the is valid and canInsert check after checking the needed amount. Should we do the same for fluids
        // or should items have the order flipped? In general calculating the needed amount is likely cheaper which is likely why items do it first
        if (stack.isEmpty() || !isFluidValid(stack) || !canInsert.test(stack, automationType)) {
            //"Fail quick" if the given stack is empty, or we can never insert the fluid or currently are unable to insert it
            return stack;
        }
        AttachedFluids attachedFluids = getAttached();
        if (attachedFluids == null) {
            //"Fail quick" if we can't have a stack
            return stack;
        }
        FluidStack stored = getContents(attachedFluids);
        int needed = Math.min(getRate(automationType), getNeeded(stored));
        if (needed <= 0) {
            //Fail if we are a full tank or our rate is zero
            return stack;
        } else if (stored.isEmpty() || FluidStack.isSameFluidSameComponents(stored, stack)) {
            int toAdd = Math.min(stack.getAmount(), needed);
            if (action.execute()) {
                //Note: We let setStack handle updating the backing holding stack
                // We use stored.getAmount + toAdd so that if we are empty we end up at toAdd
                // but if we aren't then we grow by the given amount
                setContents(attachedFluids, stack.copyWithAmount(stored.getAmount() + toAdd));
            }
            return stack.copyWithAmount(stack.getAmount() - toAdd);
        }
        //If we didn't accept this fluid, then just return the given stack
        return stack;
    }

    @Override
    public final FluidStack extract(int amount, Action action, AutomationType automationType) {
        if (amount < 1) {
            //"Fail quick" if the amount being requested is less than one
            return FluidStack.EMPTY;
        }
        AttachedFluids attachedFluids = getAttached();
        if (attachedFluids == null) {
            //"Fail quick" if we can't have a stack
            return FluidStack.EMPTY;
        }
        return extract(attachedFluids, getContents(attachedFluids), amount, action, automationType);
    }

    protected FluidStack extract(AttachedFluids attachedFluids, FluidStack stored, int amount, Action action, AutomationType automationType) {
        if (amount < 1 || stored.isEmpty() || !canExtract.test(stored, automationType)) {
            //"Fail quick" if we don't can never extract from this tank, have a fluid stored, or the amount being requested is less than one
            return FluidStack.EMPTY;
        }
        //Note: While we technically could just return the stack itself if we are removing all that we have, it would require a lot more checks
        // We also are limiting it by the rate this tank has
        int size = Math.min(Math.min(getRate(automationType), stored.getAmount()), amount);
        FluidStack ret = stored.copyWithAmount(size);
        if (!ret.isEmpty() && action.execute()) {
            //Note: We let setStack handle updating the backing holding stack
            setContents(attachedFluids, stored.copyWithAmount(stored.getAmount() - ret.getAmount()));
        }
        return ret;
    }

    @Override
    public final int setStackSize(int amount, Action action) {
        AttachedFluids attachedFluids = getAttached();
        if (attachedFluids == null) {
            //"Fail quick" if we can't have a stack
            return 0;
        }
        return setStackSize(attachedFluids, getContents(attachedFluids), amount, action);
    }

    protected int setStackSize(AttachedFluids attachedFluids, FluidStack stored, int amount, Action action) {
        if (stored.isEmpty()) {
            return 0;
        } else if (amount <= 0) {
            if (action.execute()) {
                setContents(attachedFluids, FluidStack.EMPTY);
            }
            return 0;
        }
        int maxStackSize = getCapacity();
        if (amount > maxStackSize) {
            amount = maxStackSize;
        }
        if (stored.getAmount() == amount || action.simulate()) {
            //If our size is not changing, or we are only simulating the change, don't do anything
            return amount;
        }
        setContents(attachedFluids, stored.copyWithAmount(amount));
        return amount;
    }

    @Override
    public int growStack(int amount, Action action) {
        AttachedFluids attachedFluids = getAttached();
        if (attachedFluids == null) {
            //"Fail quick" if we can't have a stack
            return 0;
        }
        FluidStack stored = getContents(attachedFluids);
        int current = stored.getAmount();
        if (amount > 0) {
            //Cap adding amount at how much we need, so that we don't risk integer overflow
            amount = Math.min(Math.min(amount, getNeeded(stored)), getRate(null));
        } else if (amount < 0) {
            amount = Math.max(amount, -getRate(null));
        }
        int newSize = setStackSize(attachedFluids, stored,current + amount, action);
        return newSize - current;
    }

    protected int getNeeded(FluidStack stored) {
        //Skip the stack lookup for getNeeded
        return Math.max(0, getCapacity() - stored.getAmount());
    }

    @Override
    public boolean isFluidValid(FluidStack stack) {
        return validator.test(stack);
    }

    @Override
    public CompoundTag serializeNBT(Provider provider) {
        //TODO - 1.20.5: This is a copy of BasicFluidTank#serializeNBT. We might need to also grab the specific overrides of
        // that method as special component backed inventory slots, that then access and put that other data as a different component?
        CompoundTag nbt = new CompoundTag();
        FluidStack stored = getFluid();
        if (!stored.isEmpty()) {
            nbt.put(NBTConstants.STORED, stored.save(provider));
        }
        return nbt;
    }

    @Override
    public void deserializeNBT(Provider provider, CompoundTag nbt) {
        NBTUtils.setFluidStackIfPresent(provider, nbt, NBTConstants.STORED, this::setStackUnchecked);
    }

    @Override
    @Deprecated
    public FluidStack drain(FluidStack stack, FluidAction action) {
        //Override to only look up the stack once
        AttachedFluids attachedFluids = getAttached();
        if (attachedFluids == null) {
            //"Fail quick" if we can't have a stack
            return FluidStack.EMPTY;
        }
        FluidStack stored = getContents(attachedFluids);
        if (!stored.isEmpty() && FluidStack.isSameFluidSameComponents(stored, stack)) {
            return extract(attachedFluids, stored, stack.getAmount(), Action.fromFluidAction(action), AutomationType.EXTERNAL);
        }
        return FluidStack.EMPTY;
    }
}