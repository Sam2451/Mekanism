package mekanism.common.item.gear;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import mekanism.api.Action;
import mekanism.api.AutomationType;
import mekanism.api.energy.IEnergyContainer;
import mekanism.api.event.MekanismTeleportEvent;
import mekanism.api.gear.ICustomModule;
import mekanism.api.gear.IModule;
import mekanism.api.gear.IModuleContainer;
import mekanism.api.gear.IModuleHelper;
import mekanism.api.math.FloatingLong;
import mekanism.api.text.EnumColor;
import mekanism.client.key.MekKeyHandler;
import mekanism.client.key.MekanismKeyHandler;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.attachments.containers.energy.ComponentBackedNoClampEnergyContainer;
import mekanism.common.attachments.containers.energy.EnergyContainersBuilder;
import mekanism.common.config.MekanismConfig;
import mekanism.common.content.gear.IBlastingItem;
import mekanism.common.content.gear.IRadialModuleContainerItem;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.content.gear.mekatool.ModuleAttackAmplificationUnit;
import mekanism.common.content.gear.mekatool.ModuleBlastingUnit;
import mekanism.common.content.gear.mekatool.ModuleExcavationEscalationUnit;
import mekanism.common.content.gear.mekatool.ModuleTeleportationUnit;
import mekanism.common.content.gear.mekatool.ModuleVeinMiningUnit;
import mekanism.common.content.gear.shared.ModuleEnergyUnit;
import mekanism.common.item.ItemEnergized;
import mekanism.common.lib.attribute.AttributeCache;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_client.PacketPortalFX;
import mekanism.common.registries.MekanismModules;
import mekanism.common.tags.MekanismTags;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.StorageUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour.BlockStateBase;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.ToolAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemMekaTool extends ItemEnergized implements IRadialModuleContainerItem, IBlastingItem {

    private static final ResourceLocation RADIAL_ID = Mekanism.rl("meka_tool");

    private final Int2ObjectMap<AttributeCache> attributeCaches = new Int2ObjectArrayMap<>(ModuleAttackAmplificationUnit.AttackDamage.values().length);

    public ItemMekaTool(Properties properties) {
        super(MekanismConfig.gear.mekaToolBaseChargeRate, MekanismConfig.gear.mekaToolBaseEnergyCapacity, properties.rarity(Rarity.EPIC).setNoRepair()
              .component(DataComponents.TOOL, ItemAtomicDisassembler.MINE_ANY_TOOL));
    }

    @Override
    public void onDestroyed(@NotNull ItemEntity item, @NotNull DamageSource damageSource) {
        ModuleHelper.INSTANCE.dropModuleContainerContents(item, damageSource);
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull Item.TooltipContext context, @NotNull List<Component> tooltip, @NotNull TooltipFlag flag) {
        if (MekKeyHandler.isKeyPressed(MekanismKeyHandler.detailsKey)) {
            addModuleDetails(stack, tooltip);
        } else {
            StorageUtils.addStoredEnergy(stack, tooltip, true);
            tooltip.add(MekanismLang.HOLD_FOR_MODULES.translateColored(EnumColor.GRAY, EnumColor.INDIGO, MekanismKeyHandler.detailsKey.getTranslatedKeyMessage()));
        }
    }

    @Override
    public boolean canPerformAction(ItemStack stack, ToolAction action) {
        if (ItemAtomicDisassembler.ALWAYS_SUPPORTED_ACTIONS.contains(action)) {
            IModuleContainer container = moduleContainer(stack);
            return container != null && hasEnergyForDigAction(container, StorageUtils.getEnergyContainer(stack, 0));
        }
        for (IModule<?> module : getModules(stack)) {
            if (module.isEnabled() && canPerformAction(module, action)) {
                return true;
            }
        }
        return false;
    }

    private <MODULE extends ICustomModule<MODULE>> boolean canPerformAction(IModule<MODULE> module, ToolAction action) {
        return module.getCustomInstance().canPerformAction(module, action);
    }

    public static boolean hasEnergyForDigAction(IModuleContainer container, @Nullable IEnergyContainer energyContainer) {
        if (energyContainer != null) {
            //Note: We use a hardness of zero here as that will get the minimum potential destroy energy required
            // as that is the best guess we can currently give whether the corresponding dig action is supported
            FloatingLong energyRequired = getDestroyEnergy(container, 0, container.hasEnabled(MekanismModules.SILK_TOUCH_UNIT));
            FloatingLong energyAvailable = energyContainer.getEnergy();
            //If we don't have enough energy to break at full speed check if the reduced speed could actually mine
            return energyRequired.smallerOrEqual(energyAvailable) || !energyAvailable.divide(energyRequired).isZero();
        }
        return false;
    }

    public static FloatingLong getDestroyEnergy(IModuleContainer container, float hardness, boolean silk) {
        return getDestroyEnergy(getDestroyEnergy(container, silk), hardness);
    }

    private static FloatingLong getDestroyEnergy(IModuleContainer container, boolean silk) {
        FloatingLong destroyEnergy = getDestroyEnergy(silk);
        IModule<ModuleExcavationEscalationUnit> module = container.getIfEnabled(MekanismModules.EXCAVATION_ESCALATION_UNIT);
        float efficiency = module == null ? MekanismConfig.gear.mekaToolBaseEfficiency.get() : module.getCustomInstance().getEfficiency();
        return destroyEnergy.multiply(efficiency);
    }

    @Override
    public boolean isNotReplaceableByPickAction(ItemStack stack, Player player, int inventorySlot) {
        //Try to avoid replacing this item if there are any modules currently installed
        return super.isNotReplaceableByPickAction(stack, player, inventorySlot) || hasInstalledModules(stack);
    }

    @Override
    public int getEnchantmentLevel(ItemStack stack, Enchantment enchantment) {
        //Enchantments in our data
        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainerNullable(stack);
        int moduleLevel = container == null ? 0 : container.getModuleEnchantmentLevel(enchantment);
        return Math.max(moduleLevel, super.getEnchantmentLevel(stack, enchantment));
    }

    @NotNull
    @Override
    public ItemEnchantments getAllEnchantments(@NotNull ItemStack stack) {
        ItemEnchantments enchantments = super.getAllEnchantments(stack);
        IModuleContainer container = IModuleHelper.INSTANCE.getModuleContainerNullable(stack);
        if (container != null) {
            ItemEnchantments moduleEnchantments = container.moduleBasedEnchantments();
            if (!moduleEnchantments.isEmpty()) {
                ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
                for (Object2IntMap.Entry<Holder<Enchantment>> entry : moduleEnchantments.entrySet()) {
                    mutable.upgrade(entry.getKey().value(), entry.getIntValue());
                }
                return mutable.toImmutable();
            }
        }
        return enchantments;
    }

    @NotNull
    @Override
    public InteractionResult useOn(UseOnContext context) {
        for (IModule<?> module : getModules(context.getItemInHand())) {
            if (module.isEnabled()) {
                InteractionResult result = onModuleUse(module, context);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
        }
        return super.useOn(context);
    }

    private <MODULE extends ICustomModule<MODULE>> InteractionResult onModuleUse(IModule<MODULE> module, UseOnContext context) {
        return module.getCustomInstance().onItemUse(module, context);
    }

    @NotNull
    @Override
    public InteractionResult interactLivingEntity(@NotNull ItemStack stack, @NotNull Player player, @NotNull LivingEntity entity, @NotNull InteractionHand hand) {
        for (IModule<?> module : getModules(stack)) {
            if (module.isEnabled()) {
                InteractionResult result = onModuleInteract(module, player, entity, hand);
                if (result != InteractionResult.PASS) {
                    return result;
                }
            }
        }
        return super.interactLivingEntity(stack, player, entity, hand);
    }

    private <MODULE extends ICustomModule<MODULE>> InteractionResult onModuleInteract(IModule<MODULE> module, @NotNull Player player, @NotNull LivingEntity entity,
          @NotNull InteractionHand hand) {
        return module.getCustomInstance().onInteract(module, player, entity, hand);
    }

    @Override
    public float getDestroySpeed(@NotNull ItemStack stack, @NotNull BlockState state) {
        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
        if (energyContainer == null) {
            return 0;
        }
        //Use raw hardness to get the best guess of if it is zero or not
        FloatingLong energyRequired = getDestroyEnergy(stack, state.destroySpeed, isModuleEnabled(stack, MekanismModules.SILK_TOUCH_UNIT));
        FloatingLong energyAvailable = energyContainer.extract(energyRequired, Action.SIMULATE, AutomationType.MANUAL);
        if (energyAvailable.smallerThan(energyRequired)) {
            //If we can't extract all the energy we need to break it go at base speed reduced by how much we actually have available
            return MekanismConfig.gear.mekaToolBaseEfficiency.get() * energyAvailable.divide(energyRequired).floatValue();
        }
        IModule<ModuleExcavationEscalationUnit> module = getEnabledModule(stack, MekanismModules.EXCAVATION_ESCALATION_UNIT);
        return module == null ? MekanismConfig.gear.mekaToolBaseEfficiency.get() : module.getCustomInstance().getEfficiency();
    }

    @Override
    public boolean mineBlock(@NotNull ItemStack stack, @NotNull Level world, @NotNull BlockState state, @NotNull BlockPos pos, @NotNull LivingEntity entityliving) {
        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
        if (energyContainer != null) {
            FloatingLong energyRequired = getDestroyEnergy(stack, state.getDestroySpeed(world, pos), isModuleEnabled(stack, MekanismModules.SILK_TOUCH_UNIT));
            energyContainer.extract(energyRequired, Action.EXECUTE, AutomationType.MANUAL);
        }
        return true;
    }

    @Override
    public boolean hurtEnemy(@NotNull ItemStack stack, @NotNull LivingEntity target, @NotNull LivingEntity attacker) {
        IModule<ModuleAttackAmplificationUnit> attackAmplificationUnit = getEnabledModule(stack, MekanismModules.ATTACK_AMPLIFICATION_UNIT);
        if (attackAmplificationUnit != null) {
            //Note: We only have an energy cost if the damage is above base, so we can skip all those checks
            // if we don't have an enabled attack amplification unit
            int unitDamage = attackAmplificationUnit.getCustomInstance().getDamage();
            if (unitDamage > 0) {
                IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
                if (energyContainer != null && !energyContainer.isEmpty()) {
                    //Try to extract full energy, even if we have a lower damage amount this is fine as that just means
                    // we don't have enough energy, but we will remove as much as we can, which is how much corresponds
                    // to the amount of damage we will actually do
                    energyContainer.extract(MekanismConfig.gear.mekaToolEnergyUsageWeapon.get().multiply(unitDamage / 4D), Action.EXECUTE, AutomationType.MANUAL);
                }
            }
        }
        return true;
    }

    @Override
    public Map<BlockPos, BlockState> getBlastedBlocks(Level world, Player player, ItemStack stack, BlockPos pos, BlockState state) {
        //Setup initial set for blasting
        if (!player.isShiftKeyDown()) {
            IModule<ModuleBlastingUnit> blastingUnit = getEnabledModule(stack, MekanismModules.BLASTING_UNIT);
            if (blastingUnit != null) {
                int radius = blastingUnit.getCustomInstance().getBlastRadius();
                if (radius > 0 && IBlastingItem.canBlastBlock(world, pos, state)) {
                    return IBlastingItem.findPositions(world, pos, player, radius);
                }
            }
        }
        return Collections.emptyMap();
    }

    private Object2IntMap<BlockPos> getVeinedBlocks(Level world, ItemStack stack, Map<BlockPos, BlockState> blocks, Reference2BooleanMap<Block> oreTracker) {
        IModule<ModuleVeinMiningUnit> veinMiningUnit = getEnabledModule(stack, MekanismModules.VEIN_MINING_UNIT);
        if (veinMiningUnit != null) {
            ModuleVeinMiningUnit customInstance = veinMiningUnit.getCustomInstance();
            return ModuleVeinMiningUnit.findPositions(world, blocks, customInstance.isExtended() ? customInstance.getExcavationRange() : 0, oreTracker);
        }
        return blocks.entrySet().stream().collect(Collectors.toMap(Entry::getKey, be -> 0, (l, r) -> l, Object2IntArrayMap::new));
    }

    @Override
    public boolean onBlockStartBreak(ItemStack stack, BlockPos pos, Player player) {
        if (player.level().isClientSide || player.isCreative()) {
            return super.onBlockStartBreak(stack, pos, player);
        }
        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
        if (energyContainer != null) {
            Level world = player.level();
            BlockState state = world.getBlockState(pos);
            boolean silk = isModuleEnabled(stack, MekanismModules.SILK_TOUCH_UNIT);
            FloatingLong modDestroyEnergy = getDestroyEnergy(stack, silk);
            FloatingLong energyRequired = getDestroyEnergy(modDestroyEnergy, state.getDestroySpeed(world, pos));
            if (energyContainer.extract(energyRequired, Action.SIMULATE, AutomationType.MANUAL).greaterOrEqual(energyRequired)) {
                Map<BlockPos, BlockState> blocks = getBlastedBlocks(world, player, stack, pos, state);
                blocks = blocks.isEmpty() && ModuleVeinMiningUnit.canVeinBlock(state) ? Map.of(pos, state) : blocks;

                Reference2BooleanMap<Block> oreTracker = blocks.values().stream().collect(Collectors.toMap(BlockStateBase::getBlock,
                      bs -> bs.is(MekanismTags.Blocks.ATOMIC_DISASSEMBLER_ORE), (l, r) -> l, Reference2BooleanArrayMap::new));

                Object2IntMap<BlockPos> veinedBlocks = getVeinedBlocks(world, stack, blocks, oreTracker);
                if (!veinedBlocks.isEmpty()) {
                    //Don't include bonus energy required by efficiency modules when calculating energy of vein mining targets
                    FloatingLong baseDestroyEnergy = getDestroyEnergy(silk);
                    MekanismUtils.veinMineArea(energyContainer, energyRequired, modDestroyEnergy, baseDestroyEnergy, world, pos, (ServerPlayer) player, stack, this, veinedBlocks,
                          ItemMekaTool::getDestroyEnergy, (base, hardness, distance, bs) -> getDestroyEnergy(base, hardness)
                                .multiply(0.5 * Math.pow(distance, bs.is(MekanismTags.Blocks.ATOMIC_DISASSEMBLER_ORE) ? 1.5 : 2)));
                }
            }
        }
        return super.onBlockStartBreak(stack, pos, player);
    }

    private static FloatingLong getDestroyEnergy(boolean silk) {
        return silk ? MekanismConfig.gear.mekaToolEnergyUsageSilk.get() : MekanismConfig.gear.mekaToolEnergyUsage.get();
    }

    public static FloatingLong getDestroyEnergy(ItemStack itemStack, float hardness, boolean silk) {
        return getDestroyEnergy(getDestroyEnergy(itemStack, silk), hardness);
    }

    private static FloatingLong getDestroyEnergy(FloatingLong baseDestroyEnergy, float hardness) {
        return hardness == 0 ? baseDestroyEnergy.divide(2) : baseDestroyEnergy;
    }

    private static FloatingLong getDestroyEnergy(ItemStack itemStack, boolean silk) {
        FloatingLong destroyEnergy = getDestroyEnergy(silk);
        IModule<ModuleExcavationEscalationUnit> module = IModuleHelper.INSTANCE.getIfEnabled(itemStack, MekanismModules.EXCAVATION_ESCALATION_UNIT);
        float efficiency = module == null ? MekanismConfig.gear.mekaToolBaseEfficiency.get() : module.getCustomInstance().getEfficiency();
        return destroyEnergy.multiply(efficiency);
    }

    @NotNull
    @Override
    public ItemAttributeModifiers getAttributeModifiers(@NotNull ItemStack stack) {
        int unitDamage = 0;
        IModule<ModuleAttackAmplificationUnit> attackAmplificationUnit = getEnabledModule(stack, MekanismModules.ATTACK_AMPLIFICATION_UNIT);
        if (attackAmplificationUnit != null) {
            unitDamage = attackAmplificationUnit.getCustomInstance().getDamage();
            if (unitDamage > 0) {
                FloatingLong energyCost = MekanismConfig.gear.mekaToolEnergyUsageWeapon.get().multiply(unitDamage / 4D);
                IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
                FloatingLong energy = energyContainer == null ? FloatingLong.ZERO : energyContainer.getEnergy();
                if (energy.smallerThan(energyCost)) {
                    //If we don't have enough power use it at a reduced power level (this will be false the majority of the time)
                    double bonusDamage = unitDamage * energy.divideToLevel(energyCost);
                    if (bonusDamage > 0) {
                        //If we actually have bonus damage (as we might not if we don't have any energy stored, and then
                        // we can just use the cache for as if there was no bonus damage)
                        ImmutableList.Builder<ItemAttributeModifiers.Entry> builder = ImmutableList.builder();
                        builder.add(new ItemAttributeModifiers.Entry(
                              Attributes.ATTACK_DAMAGE,
                              new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", MekanismConfig.gear.mekaToolBaseDamage.get() + bonusDamage, Operation.ADD_VALUE),
                              EquipmentSlotGroup.MAINHAND
                        ));
                        builder.add(new ItemAttributeModifiers.Entry(
                              Attributes.ATTACK_SPEED,
                              new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", MekanismConfig.gear.mekaToolAttackSpeed.get(), Operation.ADD_VALUE),
                              EquipmentSlotGroup.MAINHAND
                        ));
                        return new ItemAttributeModifiers(builder.build(), true);
                    }
                    //Use cached attribute map for just doing the base damage
                    unitDamage = 0;
                }
            }
        }
        //Retrieve a cached map if we have enough energy to attack at the full damage value based on configured damage
        return attributeCaches.computeIfAbsent(unitDamage, damage -> new AttributeCache(builder -> {
            builder.add(new ItemAttributeModifiers.Entry(
                  Attributes.ATTACK_DAMAGE,
                  new AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier", MekanismConfig.gear.mekaToolBaseDamage.get() + damage, Operation.ADD_VALUE),
                  EquipmentSlotGroup.MAINHAND
            ));
            builder.add(new ItemAttributeModifiers.Entry(
                  Attributes.ATTACK_SPEED,
                  new AttributeModifier(BASE_ATTACK_SPEED_UUID, "Weapon modifier", MekanismConfig.gear.mekaToolAttackSpeed.get(), Operation.ADD_VALUE),
                  EquipmentSlotGroup.MAINHAND
            ));
        }, MekanismConfig.gear.mekaToolBaseDamage, MekanismConfig.gear.mekaToolAttackSpeed)).get();
    }

    @NotNull
    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, @NotNull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!world.isClientSide()) {
            IModule<ModuleTeleportationUnit> module = getEnabledModule(stack, MekanismModules.TELEPORTATION_UNIT);
            if (module != null) {
                BlockHitResult result = MekanismUtils.rayTrace(player, MekanismConfig.gear.mekaToolMaxTeleportReach.get());
                //If we don't require a block target or are not a miss, allow teleporting
                if (!module.getCustomInstance().requiresBlockTarget() || result.getType() != HitResult.Type.MISS) {
                    BlockPos pos = result.getBlockPos();
                    // make sure we fit
                    if (isValidDestinationBlock(world, pos.above()) && isValidDestinationBlock(world, pos.above(2))) {
                        double distance = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
                        if (distance < 5) {
                            return InteractionResultHolder.pass(stack);
                        }
                        IEnergyContainer energyContainer = StorageUtils.getEnergyContainer(stack, 0);
                        FloatingLong energyNeeded = MekanismConfig.gear.mekaToolEnergyUsageTeleport.get().multiply(distance / 10D);
                        if (energyContainer == null || energyContainer.getEnergy().smallerThan(energyNeeded)) {
                            return InteractionResultHolder.fail(stack);
                        }
                        double targetX = pos.getX() + 0.5;
                        double targetY = pos.getY() + 1.5;
                        double targetZ = pos.getZ() + 0.5;
                        MekanismTeleportEvent.MekaTool event = new MekanismTeleportEvent.MekaTool(player, targetX, targetY, targetZ, stack, result);
                        if (NeoForge.EVENT_BUS.post(event).isCanceled()) {
                            //Fail if the event was cancelled
                            return InteractionResultHolder.fail(stack);
                        }
                        //Note: We intentionally don't use the event's coordinates as we do not support changing the location the Meka-Tool is teleporting to
                        energyContainer.extract(energyNeeded, Action.EXECUTE, AutomationType.MANUAL);
                        if (player.isPassenger()) {
                            player.dismountTo(targetX, targetY, targetZ);
                        } else {
                            player.teleportTo(targetX, targetY, targetZ);
                        }
                        player.resetFallDistance();
                        PacketUtils.sendToAllTracking(new PacketPortalFX(pos.above()), world, pos);
                        world.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
                        return InteractionResultHolder.success(stack);
                    }
                }
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    private boolean isValidDestinationBlock(Level world, BlockPos pos) {
        BlockState blockState = world.getBlockState(pos);
        //Allow teleporting into air or fluids
        return blockState.isAir() || MekanismUtils.isLiquidBlock(blockState.getBlock());
    }

    @Override
    public boolean isEnchantable(@NotNull ItemStack stack) {
        return false;
    }

    @Override
    public boolean isBookEnchantable(ItemStack stack, ItemStack book) {
        return false;
    }

    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack, Enchantment enchantment) {
        return false;
    }

    @Override
    protected EnergyContainersBuilder addDefaultEnergyContainers(EnergyContainersBuilder builder) {
        return builder.addContainer((type, attachedTo, containerIndex) -> new ComponentBackedNoClampEnergyContainer(attachedTo, containerIndex, canExtract, canInsert,
              () -> ModuleEnergyUnit.getChargeRate(attachedTo, MekanismConfig.gear.mekaToolBaseChargeRate.get()),
              () -> ModuleEnergyUnit.getEnergyCapacity(attachedTo, MekanismConfig.gear.mekaToolBaseEnergyCapacity.get())));
    }

    @Override
    public ResourceLocation getRadialIdentifier() {
        return RADIAL_ID;
    }
}