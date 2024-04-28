package mekanism.common.item.block;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import mekanism.api.RelativeSide;
import mekanism.api.text.EnumColor;
import mekanism.client.render.RenderPropertiesProvider;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.component.AttachedSideConfig;
import mekanism.common.attachments.component.AttachedSideConfig.LightConfigInfo;
import mekanism.common.attachments.containers.energy.ComponentBackedEnergyCubeContainer;
import mekanism.common.attachments.containers.energy.EnergyContainersBuilder;
import mekanism.common.block.BlockEnergyCube;
import mekanism.common.block.attribute.Attribute;
import mekanism.common.config.MekanismConfig;
import mekanism.common.lib.transmitter.TransmissionType;
import mekanism.common.registration.impl.CreativeTabDeferredRegister.ICustomCreativeTabContents;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tier.EnergyCubeTier;
import mekanism.common.tile.component.config.DataType;
import mekanism.common.util.EnumUtils;
import mekanism.common.util.StorageUtils;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.NotNull;

public class ItemBlockEnergyCube extends ItemBlockTooltip<BlockEnergyCube> implements ICustomCreativeTabContents {

    private static final AttachedSideConfig ALL_INPUT = Util.make(() -> {
        Map<RelativeSide, DataType> sideData = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : EnumUtils.SIDES) {
            sideData.put(side, DataType.INPUT);
        }
        return new AttachedSideConfig(Map.of(TransmissionType.ENERGY, new LightConfigInfo(sideData, null)));
    });
    private static final AttachedSideConfig ALL_OUTPUT = Util.make(() -> {
        Map<RelativeSide, DataType> sideData = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : EnumUtils.SIDES) {
            sideData.put(side, DataType.OUTPUT);
        }
        return new AttachedSideConfig(Map.of(TransmissionType.ENERGY, new LightConfigInfo(sideData, null)));
    });

    public ItemBlockEnergyCube(BlockEnergyCube block) {
        super(block);
    }

    @Override
    public void initializeClient(@NotNull Consumer<IClientItemExtensions> consumer) {
        consumer.accept(RenderPropertiesProvider.energyCube());
    }

    @NotNull
    @Override
    public EnergyCubeTier getTier() {
        return Attribute.getTier(getBlock(), EnergyCubeTier.class);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        StorageUtils.addStoredEnergy(stack, tooltip, true);
        tooltip.add(MekanismLang.CAPACITY.translateColored(EnumColor.INDIGO, EnumColor.GRAY, EnergyDisplay.of(getTier().getMaxEnergy())));
        super.appendHoverText(stack, context, tooltip, flag);
    }

    @Override
    protected void addTypeDetails(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        //Don't call super so that we can exclude the stored energy from being shown as we show it in hover text
    }

    @Override
    public boolean isBarVisible(@NotNull ItemStack stack) {
        //If we are currently stacked, don't display the bar as it will overlap the stack count
        return stack.getCount() == 1;
    }

    @Override
    public int getBarWidth(@NotNull ItemStack stack) {
        return StorageUtils.getEnergyBarWidth(stack);
    }

    @Override
    public int getBarColor(@NotNull ItemStack stack) {
        return MekanismConfig.client.energyColor.get();
    }

    @Override
    public void addItems(CreativeModeTab.Output tabOutput) {
        EnergyCubeTier tier = getTier();
        if (tier == EnergyCubeTier.CREATIVE) {
            //Add the empty and charged variants
            tabOutput.accept(withEnergyCubeSideConfig(ALL_INPUT));
            tabOutput.accept(StorageUtils.getFilledEnergyVariant(withEnergyCubeSideConfig(ALL_OUTPUT)));
        } else {
            tabOutput.accept(StorageUtils.getFilledEnergyVariant(new ItemStack(this)));
        }
    }

    @Override
    public boolean addDefault() {
        return getTier() != EnergyCubeTier.CREATIVE;
    }

    private ItemStack withEnergyCubeSideConfig(AttachedSideConfig config) {
        ItemStack stack = new ItemStack(this);
        stack.set(MekanismDataComponents.SIDE_CONFIG, config);
        return stack;
    }

    @Override
    protected EnergyContainersBuilder addDefaultEnergyContainers(EnergyContainersBuilder builder) {
        return builder.addContainer(ComponentBackedEnergyCubeContainer::create);
    }
}