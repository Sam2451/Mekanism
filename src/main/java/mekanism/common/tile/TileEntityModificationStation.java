package mekanism.common.tile;

import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.IContentsListener;
import mekanism.api.NBTConstants;
import mekanism.api.RelativeSide;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.gear.ModuleData;
import mekanism.api.math.FloatingLong;
import mekanism.common.capabilities.energy.MachineEnergyContainer;
import mekanism.common.capabilities.holder.energy.EnergyContainerHelper;
import mekanism.common.capabilities.holder.energy.IEnergyContainerHolder;
import mekanism.common.capabilities.holder.slot.IInventorySlotHolder;
import mekanism.common.capabilities.holder.slot.InventorySlotHelper;
import mekanism.common.content.gear.IModuleItem;
import mekanism.common.content.gear.ModuleContainer;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.integration.computer.SpecialComputerMethodWrapper.ComputerIInventorySlotWrapper;
import mekanism.common.integration.computer.annotation.WrappingComputerMethod;
import mekanism.common.inventory.container.MekanismContainer;
import mekanism.common.inventory.container.slot.ContainerSlotType;
import mekanism.common.inventory.container.slot.SlotOverlay;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.slot.EnergyInventorySlot;
import mekanism.common.inventory.slot.InputInventorySlot;
import mekanism.common.registries.MekanismBlocks;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.interfaces.IBoundingBlock;
import mekanism.common.util.MekanismUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public class TileEntityModificationStation extends TileEntityMekanism implements IBoundingBlock {

    private static final int BASE_TICKS_REQUIRED = MekanismUtils.TICKS_PER_HALF_SECOND;

    public int ticksRequired = BASE_TICKS_REQUIRED;
    public int operatingTicks;
    private boolean usedEnergy = false;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getEnergyItem", docPlaceholder = "energy slot")
    EnergyInventorySlot energySlot;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getModuleItem", docPlaceholder = "module slot")
    InputInventorySlot moduleSlot;
    @WrappingComputerMethod(wrapper = ComputerIInventorySlotWrapper.class, methodNames = "getContainerItem", docPlaceholder = "module holder slot (suit, tool, etc)")
    public InputInventorySlot containerSlot;
    private MachineEnergyContainer<TileEntityModificationStation> energyContainer;

    public TileEntityModificationStation(BlockPos pos, BlockState state) {
        super(MekanismBlocks.MODIFICATION_STATION, pos, state);
    }

    @NotNull
    @Override
    protected IEnergyContainerHolder getInitialEnergyContainers(IContentsListener listener) {
        EnergyContainerHelper builder = EnergyContainerHelper.forSide(this::getDirection);
        builder.addContainer(energyContainer = MachineEnergyContainer.input(this, listener), RelativeSide.BACK);
        return builder.build();
    }

    public MachineEnergyContainer<TileEntityModificationStation> getEnergyContainer() {
        return energyContainer;
    }

    @NotNull
    @Override
    protected IInventorySlotHolder getInitialInventory(IContentsListener listener) {
        InventorySlotHelper builder = InventorySlotHelper.forSide(this::getDirection);
        builder.addSlot(moduleSlot = InputInventorySlot.at(stack -> stack.getItem() instanceof IModuleItem, listener, 35, 118));
        builder.addSlot(containerSlot = InputInventorySlot.at(IModuleHelper.INSTANCE::isModuleContainer, listener, 125, 118));
        moduleSlot.setSlotType(ContainerSlotType.NORMAL);
        moduleSlot.setSlotOverlay(SlotOverlay.MODULE);
        containerSlot.setSlotType(ContainerSlotType.NORMAL);
        builder.addSlot(energySlot = EnergyInventorySlot.fillOrConvert(energyContainer, this::getLevel, listener, 149, 21));
        return builder.build();
    }

    @Override
    protected boolean onUpdateServer() {
        boolean sendUpdatePacket = super.onUpdateServer();
        energySlot.fillContainerOrConvert();
        FloatingLong clientEnergyUsed = FloatingLong.ZERO;
        if (canFunction()) {
            boolean operated = false;
            if (energyContainer.getEnergy().greaterOrEqual(energyContainer.getEnergyPerTick()) && !moduleSlot.isEmpty() && !containerSlot.isEmpty()) {
                ModuleData<?> data = ((IModuleItem) moduleSlot.getStack().getItem()).getModuleData();
                ItemStack stack = containerSlot.getStack();
                ModuleContainer container = ModuleHelper.get().getModuleContainer(stack);
                if (container != null) {
                    // make sure the container supports this module and that we can still install more of this module
                    if (container.canInstall(data)) {
                        operated = true;
                        operatingTicks++;
                        clientEnergyUsed = energyContainer.extract(energyContainer.getEnergyPerTick(), Action.EXECUTE, AutomationType.INTERNAL);
                        if (operatingTicks == ticksRequired) {
                            operatingTicks = 0;
                            int added = container.addModule(data, moduleSlot.getCount());
                            if (added > 0) {
                                containerSlot.setStack(stack);
                                MekanismUtils.logMismatchedStackSize(moduleSlot.shrinkStack(added, Action.EXECUTE), added);
                            }
                        }
                    }
                }
            }
            if (!operated) {
                operatingTicks = 0;
            }
        }
        usedEnergy = !clientEnergyUsed.isZero();
        return sendUpdatePacket;
    }

    public boolean usedEnergy() {
        return usedEnergy;
    }

    public void removeModule(Player player, ModuleData<?> type, boolean removeAll) {
        ItemStack stack = containerSlot.getStack();
        ModuleContainer container = ModuleHelper.get().getModuleContainer(stack);
        if (container != null) {
            if (container.has(type)) {
                int toRemove = removeAll ? container.installedCount(type) : 1;
                if (player.getInventory().add(type.getItemProvider().getItemStack(toRemove))) {
                    container.removeModule(type, toRemove);
                    containerSlot.setStack(stack);
                }
            }
        }
    }

    public double getScaledProgress() {
        return (double) operatingTicks / ticksRequired;
    }

    @Override
    public void loadAdditional(@NotNull CompoundTag nbt, @NotNull HolderLookup.Provider provider) {
        super.loadAdditional(nbt, provider);
        operatingTicks = nbt.getInt(NBTConstants.PROGRESS);
    }

    @Override
    public void saveAdditional(@NotNull CompoundTag nbtTags, @NotNull HolderLookup.Provider provider) {
        super.saveAdditional(nbtTags, provider);
        nbtTags.putInt(NBTConstants.PROGRESS, operatingTicks);
    }

    @Override
    public void addContainerTrackers(MekanismContainer container) {
        super.addContainerTrackers(container);
        container.track(SyncableInt.create(() -> operatingTicks, value -> operatingTicks = value));
        container.track(SyncableBoolean.create(this::usedEnergy, value -> usedEnergy = value));
    }
}
