package mekanism.common.attachments.containers.chemical.gas;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collections;
import java.util.List;
import mekanism.api.NBTConstants;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.chemical.gas.GasStack;
import mekanism.common.attachments.containers.IAttachedContainers;
import net.minecraft.core.NonNullList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

@NothingNullByDefault
public record AttachedGases(List<GasStack> containers) implements IAttachedContainers<GasStack, AttachedGases> {

    public static final Codec<AttachedGases> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          GasStack.OPTIONAL_CODEC.listOf().fieldOf(NBTConstants.GAS_TANKS).forGetter(AttachedGases::containers)
    ).apply(instance, AttachedGases::new));
    public static final StreamCodec<RegistryFriendlyByteBuf, AttachedGases> STREAM_CODEC =
          GasStack.OPTIONAL_STREAM_CODEC.<List<GasStack>>apply(ByteBufCodecs.collection(NonNullList::createWithCapacity))
                      .map(AttachedGases::new, AttachedGases::containers);

    public AttachedGases {
        //Make the list unmodifiable to ensure we don't accidentally mutate it
        containers = Collections.unmodifiableList(containers);
    }

    public AttachedGases(int containers) {
        this(NonNullList.withSize(containers, GasStack.EMPTY));
    }

    @Override
    public AttachedGases create(List<GasStack> containers) {
        return new AttachedGases(containers);
    }
}