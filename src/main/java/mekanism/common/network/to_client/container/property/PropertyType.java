package mekanism.common.network.to_client.container.property;

import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import java.util.function.Supplier;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.infuse.InfusionStack;
import mekanism.api.chemical.pigment.PigmentStack;
import mekanism.api.chemical.slurry.SlurryStack;
import mekanism.api.math.FloatingLong;
import mekanism.common.inventory.container.sync.ISyncableData;
import mekanism.common.inventory.container.sync.SyncableBlockPos;
import mekanism.common.inventory.container.sync.SyncableBoolean;
import mekanism.common.inventory.container.sync.SyncableByte;
import mekanism.common.inventory.container.sync.SyncableByteArray;
import mekanism.common.inventory.container.sync.SyncableDouble;
import mekanism.common.inventory.container.sync.SyncableFloat;
import mekanism.common.inventory.container.sync.SyncableFloatingLong;
import mekanism.common.inventory.container.sync.SyncableFluidStack;
import mekanism.common.inventory.container.sync.SyncableInt;
import mekanism.common.inventory.container.sync.SyncableItemStack;
import mekanism.common.inventory.container.sync.SyncableLong;
import mekanism.common.inventory.container.sync.SyncableRegistryEntry;
import mekanism.common.inventory.container.sync.SyncableShort;
import mekanism.common.inventory.container.sync.chemical.SyncableGasStack;
import mekanism.common.inventory.container.sync.chemical.SyncableInfusionStack;
import mekanism.common.inventory.container.sync.chemical.SyncablePigmentStack;
import mekanism.common.inventory.container.sync.chemical.SyncableSlurryStack;
import mekanism.common.network.to_client.container.property.chemical.GasStackPropertyData;
import mekanism.common.network.to_client.container.property.chemical.InfusionStackPropertyData;
import mekanism.common.network.to_client.container.property.chemical.PigmentStackPropertyData;
import mekanism.common.network.to_client.container.property.chemical.SlurryStackPropertyData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.Nullable;

public enum PropertyType {
    BOOLEAN(Boolean.TYPE, false, (getter, setter) -> SyncableBoolean.create(() -> (boolean) getter.get(), setter::accept), () -> BooleanPropertyData.STREAM_CODEC),
    BYTE(Byte.TYPE, (byte) 0, (getter, setter) -> SyncableByte.create(() -> (byte) getter.get(), setter::accept), () -> BytePropertyData.STREAM_CODEC),
    DOUBLE(Double.TYPE, 0D, (getter, setter) -> SyncableDouble.create(() -> (double) getter.get(), setter::accept), () -> DoublePropertyData.STREAM_CODEC),
    FLOAT(Float.TYPE, 0F, (getter, setter) -> SyncableFloat.create(() -> (float) getter.get(), setter::accept), () -> FloatPropertyData.STREAM_CODEC),
    INT(Integer.TYPE, 0, (getter, setter) -> SyncableInt.create(() -> (int) getter.get(), setter::accept), () -> IntPropertyData.STREAM_CODEC),
    LONG(Long.TYPE, 0L, (getter, setter) -> SyncableLong.create(() -> (long) getter.get(), setter::accept), () -> LongPropertyData.STREAM_CODEC),
    SHORT(Short.TYPE, (short) 0, (getter, setter) -> SyncableShort.create(() -> (short) getter.get(), setter::accept), () -> ShortPropertyData.STREAM_CODEC),
    BYTE_ARRAY(byte[].class, new byte[0], (getter, setter) -> SyncableByteArray.create(() -> (byte[]) getter.get(), setter::accept), () -> ByteArrayPropertyData.STREAM_CODEC),
    ITEM_STACK(ItemStack.class, ItemStack.EMPTY, (getter, setter) -> SyncableItemStack.create(() -> (ItemStack) getter.get(), setter::accept), () -> ItemStackPropertyData.STREAM_CODEC),
    FLUID_STACK(FluidStack.class, FluidStack.EMPTY, (getter, setter) -> SyncableFluidStack.create(() -> (FluidStack) getter.get(), setter::accept), () -> FluidStackPropertyData.STREAM_CODEC),
    GAS_STACK(GasStack.class, GasStack.EMPTY, (getter, setter) -> SyncableGasStack.create(() -> (GasStack) getter.get(), setter::accept), () -> GasStackPropertyData.STREAM_CODEC),
    INFUSION_STACK(InfusionStack.class, InfusionStack.EMPTY, (getter, setter) -> SyncableInfusionStack.create(() -> (InfusionStack) getter.get(), setter::accept), () -> InfusionStackPropertyData.STREAM_CODEC),
    PIGMENT_STACK(PigmentStack.class, PigmentStack.EMPTY, (getter, setter) -> SyncablePigmentStack.create(() -> (PigmentStack) getter.get(), setter::accept), () -> PigmentStackPropertyData.STREAM_CODEC),
    SLURRY_STACK(SlurryStack.class, SlurryStack.EMPTY, (getter, setter) -> SyncableSlurryStack.create(() -> (SlurryStack) getter.get(), setter::accept), () -> SlurryStackPropertyData.STREAM_CODEC),
    BLOCK_POS(BlockPos.class, null, (getter, setter) -> SyncableBlockPos.create(() -> (BlockPos) getter.get(), setter::accept), () -> BlockPosPropertyData.STREAM_CODEC),
    FLOATING_LONG(FloatingLong.class, FloatingLong.ZERO, (getter, setter) -> SyncableFloatingLong.create(() -> (FloatingLong) getter.get(), setter::accept), () -> FloatingLongPropertyData.STREAM_CODEC);

    public static final IntFunction<PropertyType> BY_ID = ByIdMap.continuous(PropertyType::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
    public static final StreamCodec<ByteBuf, PropertyType> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, PropertyType::ordinal);

    private final Class<?> type;
    private final Object defaultValue;
    @Nullable
    private final BiFunction<Supplier<Object>, Consumer<Object>, ISyncableData> creatorFunction;
    private final Supplier<StreamCodec<? super RegistryFriendlyByteBuf, ? extends PropertyData>> streamCodec;

    private static final PropertyType[] VALUES = values();

    //For use by any specific registry types we might at some point need the annotation for
    // potentially should have a better solution done than this
    <V> PropertyType(Class<V> type, Registry<V> registry, Supplier<StreamCodec<? super RegistryFriendlyByteBuf, ? extends PropertyData>> streamCodec) {
        this(type, null, (supplier, consumer) -> SyncableRegistryEntry.create(registry, (Supplier<V>) supplier, (Consumer<V>) consumer), streamCodec);
    }

    PropertyType(Class<?> type, Object defaultValue, @Nullable BiFunction<Supplier<Object>, Consumer<Object>, ISyncableData> creatorFunction,
          Supplier<StreamCodec<? super RegistryFriendlyByteBuf, ? extends PropertyData>> streamCodec) {
        this.type = type;
        this.defaultValue = defaultValue;
        this.creatorFunction = creatorFunction;
        this.streamCodec = streamCodec;
    }

    public <T> T getDefault() {
        return (T) defaultValue;
    }

    public static PropertyType getFromType(Class<?> type) {
        for (PropertyType propertyType : VALUES) {
            if (type == propertyType.type) {
                return propertyType;
            }
        }
        return null;
    }

    public ISyncableData create(Supplier<Object> supplier, Consumer<Object> consumer) {
        if (creatorFunction == null) {
            throw new IllegalStateException(name() + " does not support annotation based syncing.");
        }
        return creatorFunction.apply(supplier, consumer);
    }

    public StreamCodec<? super RegistryFriendlyByteBuf, ? extends PropertyData> streamCodec() {
        return streamCodec.get();
    }
}