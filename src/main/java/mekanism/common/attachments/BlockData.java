package mekanism.common.attachments;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import mekanism.api.NBTConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.text.EnumColor;
import mekanism.common.MekanismLang;
import mekanism.common.config.MekanismConfig;
import mekanism.common.util.RegistryUtils;
import mekanism.common.util.WorldUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.DecoratedPotBlock;
import net.minecraft.world.level.block.SpawnerBlock;
import net.minecraft.world.level.block.TrialSpawnerBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import org.jetbrains.annotations.Nullable;

@NothingNullByDefault
public record BlockData(BlockState blockState, @Nullable CompoundTag blockEntityTag) {

    public static final Codec<BlockData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          BlockState.CODEC.fieldOf(NBTConstants.STATE).forGetter(BlockData::blockState),
          CompoundTag.CODEC.optionalFieldOf(NBTConstants.BE_TAG).forGetter(data -> Optional.ofNullable(data.blockEntityTag))
    ).apply(instance, (state, tag) -> new BlockData(state, tag.orElse(null))));
    //TODO - 1.20.5: Test this and see if there is a proper stream codec for block states
    public static final StreamCodec<ByteBuf, BlockData> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.TRUSTED_COMPOUND_TAG, data -> (CompoundTag) BlockState.CODEC.encodeStart(NbtOps.INSTANCE, data.blockState()).getOrThrow(),
          ByteBufCodecs.optional(ByteBufCodecs.TRUSTED_COMPOUND_TAG), data -> Optional.ofNullable(data.blockEntityTag()),
          (state, tag) -> new BlockData(BlockState.CODEC.parse(NbtOps.INSTANCE, state).result().orElseThrow(), tag.orElse(null))
    );

    public BlockData(HolderLookup.Provider provider, BlockState state, @Nullable BlockEntity blockEntity) {
        this(state, blockEntity == null ? null : blockEntity.saveWithFullMetadata(provider));
    }

    public boolean tryPlaceIntoWorld(Level level, BlockPos pos, @Nullable Player player) {
        //TODO: Note - this will not allow for rotation of the block based on how it is placed direction wise via the removal of
        // the cardboard box and will instead leave it how it was when the box was initially put on
        //Adjust the state based on neighboring blocks to ensure double chests properly become single chests again
        BlockState adjustedState = Block.updateFromNeighbourShapes(blockState, level, pos);
        if (adjustedState.isAir()) {
            //If the block cannot be unpacked in this position, don't allow it to be unpacked
            return false;
        }

        FluidState fluidState = adjustedState.getFluidState();
        FluidType fluidType = fluidState.getFluidType();
        //Note: Doesn't support nbt
        FluidStack fluid = new FluidStack(fluidState.getType(), FluidType.BUCKET_VOLUME);
        BucketPickup tryPickup = null;
        //Do our best effort to support to not allow water to be placed into the nether
        if (fluidType.isVaporizedOnPlacement(level, pos, fluid)) {
            if (!MekanismConfig.general.strictUnboxing.get() && adjustedState.getBlock() instanceof BucketPickup pickup) {
                tryPickup = pickup;
            } else {
                //Not a bucket pickup, we don't know how to pick up the block
                return false;
            }
        }

        level.setBlockAndUpdate(pos, adjustedState);
        //TODO: Do we need to call setPlacedBy or not bother given we are setting the blockstate to what it was AND setting any tile data
        //adjustedState.getBlock().setPlacedBy(world, pos, blockState, player, new ItemStack(adjustedState.getBlock()));
        if (blockEntityTag != null) {
            //Update the location
            blockEntityTag.putInt(NBTConstants.X, pos.getX());
            blockEntityTag.putInt(NBTConstants.Y, pos.getY());
            blockEntityTag.putInt(NBTConstants.Z, pos.getZ());
            //And get the block entity and load it from the data
            BlockEntity tile = WorldUtils.getTileEntity(level, pos);
            if (tile != null) {
                tile.loadWithComponents(blockEntityTag, level.registryAccess());
            }
        }
        if (tryPickup != null) {
            if (!tryPickup.pickupBlock(player, level, pos, adjustedState).isEmpty()) {
                fluidType.onVaporize(null, level, pos, fluid);
            }
        }
        return true;
    }

    public void addToTooltip(Consumer<Component> consumer) {
        Block block = blockState.getBlock();
        consumer.accept(MekanismLang.BLOCK.translateColored(EnumColor.INDIGO, EnumColor.GRAY, block));
        if (blockEntityTag != null) {
            Optional<BlockEntityType<?>> blockEntityType = RegistryUtils.getById(blockEntityTag, BuiltInRegistries.BLOCK_ENTITY_TYPE);
            Object beName = blockEntityType.isPresent() ? RegistryUtils.getName(blockEntityType.get()) : MekanismLang.UNKNOWN;
            consumer.accept(MekanismLang.BLOCK_ENTITY.translateColored(EnumColor.INDIGO, EnumColor.GRAY, beName));
            if (blockEntityTag != null) {
                if (block instanceof SpawnerBlock || block instanceof TrialSpawnerBlock) {
                    String key = block instanceof SpawnerBlock ? NBTConstants.SPAWN_DATA_LEGACY : NBTConstants.SPAWN_DATA;
                    RegistryUtils.getById(blockEntityTag.getCompound(key).getCompound(NBTConstants.ENTITY), BuiltInRegistries.ENTITY_TYPE)
                          .map(entity -> MekanismLang.BLOCK_ENTITY_SPAWN_TYPE.translateColored(EnumColor.INDIGO, EnumColor.GRAY, entity))
                          .ifPresent(consumer);
                } else if (block instanceof DecoratedPotBlock) {
                    PotDecorations decorations = PotDecorations.load(blockEntityTag);
                    //Copy from DecoratedPotBlock#appendHoverText
                    if (!decorations.equals(PotDecorations.EMPTY)) {
                        consumer.accept(MekanismLang.BLOCK_ENTITY_DECORATION.translateColored(EnumColor.INDIGO));
                        Stream.of(decorations.front(), decorations.left(), decorations.right(), decorations.back())
                              .map(decoration -> MekanismLang.GENERIC_LIST.translateColored(EnumColor.INDIGO, EnumColor.GRAY, decoration))
                              .forEach(consumer);
                    }
                }
            }
        }
    }
}