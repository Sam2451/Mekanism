package mekanism.common.registration.impl;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import mekanism.api.SerializerHelper;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.math.FloatingLong;
import mekanism.common.attachments.FrequencyAware;
import mekanism.common.attachments.containers.AttachedContainers;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.lib.frequency.Frequency;
import mekanism.common.lib.frequency.FrequencyType;
import mekanism.common.registration.MekanismDeferredHolder;
import mekanism.common.registration.MekanismDeferredRegister;
import net.minecraft.core.Registry;
import net.minecraft.core.UUIDUtil;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.common.util.INBTSerializable;

//TODO - 1.20.5: Should we be setting cacheEncoding on any of our builders?
//TODO - 1.20.5: Figure out how to handle the default values
@NothingNullByDefault
public class DataComponentDeferredRegister extends MekanismDeferredRegister<DataComponentType<?>> {

    public DataComponentDeferredRegister(String namespace) {
        super(Registries.DATA_COMPONENT_TYPE, namespace);
    }

    public <TYPE> MekanismDeferredHolder<DataComponentType<?>, DataComponentType<TYPE>> simple(String name, UnaryOperator<DataComponentType.Builder<TYPE>> operator) {
        return register(name, () -> operator.apply(DataComponentType.builder()).build());
    }

    public <CONTAINER extends INBTSerializable<CompoundTag>, ATTACHMENT extends AttachedContainers<CONTAINER>>
    MekanismDeferredHolder<DataComponentType<?>, DataComponentType<ATTACHMENT>> registerContainer(String name, Supplier<ContainerType<CONTAINER, ATTACHMENT, ?>> typeSupplier) {
        return simple(name, builder -> {
            ContainerType<CONTAINER, ATTACHMENT, ?> containerType = typeSupplier.get();
            //TODO - 1.20.5: Figure out how we want to setup containers
            /*return AttachmentType.serializable(containerType::getDefault)
                  .copyHandler(containerType)
                  .build();*/
            return builder.persistent(Codec.unit(() -> containerType.getDefault(ItemStack.EMPTY)));
        });
    }

    public <FREQ extends Frequency> MekanismDeferredHolder<DataComponentType<?>, DataComponentType<FrequencyAware<FREQ>>> registerFrequencyAware(String name,
          Supplier<FrequencyType<FREQ>> frequencyTypeSupplier) {
        return simple(name, builder -> {
            FrequencyType<FREQ> frequencyType = frequencyTypeSupplier.get();
            return builder.persistent(FrequencyAware.codec(frequencyType))
                  .networkSynchronized(FrequencyAware.streamCodec(frequencyType));
        });
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Boolean>> registerBoolean(String name, boolean defaultValue) {
        return simple(name, builder -> builder.persistent(Codec.BOOL)
              .networkSynchronized(ByteBufCodecs.BOOL));
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Integer>> registerNonNegativeInt(String name, int defaultValue) {
        return simple(name, builder -> builder.persistent(ExtraCodecs.POSITIVE_INT)
              .networkSynchronized(ByteBufCodecs.VAR_INT));
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Integer>> registerInt(String name, int defaultValue, int min, int max) {
        return simple(name, builder -> builder.persistent(Codec.intRange(min, max))
              .networkSynchronized(ByteBufCodecs.VAR_INT));
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Long>> registerNonNegativeLong(String name, long defaultValue) {
        return simple(name, builder -> builder.persistent(SerializerHelper.POSITIVE_NONZERO_LONG_CODEC)
              .networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Long>> registerLong(String name, long defaultValue, long min, long max) {
        final Function<Long, DataResult<Long>> checker = Codec.checkRange(min, max);
        return simple(name, builder -> builder.persistent(Codec.LONG.flatXmap(checker, checker))
              .networkSynchronized(ByteBufCodecs.VAR_LONG));
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<FloatingLong>> registerFloatingLong(String name) {
        return simple(name, builder -> builder.persistent(FloatingLong.CODEC)
              .networkSynchronized(FloatingLong.STREAM_CODEC));
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<UUID>> registerUUID(String name) {
        return simple(name, builder -> builder.persistent(UUIDUtil.CODEC)
              .networkSynchronized(UUIDUtil.STREAM_CODEC));
    }

    public MekanismDeferredHolder<DataComponentType<?>, DataComponentType<Component>> registerComponent(String name) {
        return simple(name, builder -> builder.persistent(ComponentSerialization.CODEC)
              .networkSynchronized(ComponentSerialization.TRUSTED_STREAM_CODEC));
    }

    public <TYPE> MekanismDeferredHolder<DataComponentType<?>, DataComponentType<ResourceKey<TYPE>>> registerResourceKey(String name, ResourceKey<? extends Registry<TYPE>> registryKey) {
        return simple(name, builder -> builder.persistent(ResourceKey.codec(registryKey))
              .networkSynchronized(ResourceKey.streamCodec(registryKey)));
    }
}