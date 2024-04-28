package mekanism.common.attachments.containers.heat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import mekanism.api.NBTConstants;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record HeatCapacitorData(double heat, double capacity) {

    public static final Codec<HeatCapacitorData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
          Codec.DOUBLE.fieldOf(NBTConstants.STORED).forGetter(HeatCapacitorData::heat),
          Codec.DOUBLE.fieldOf(NBTConstants.HEAT_CAPACITY).forGetter(HeatCapacitorData::capacity)
    ).apply(instance, HeatCapacitorData::new));
    public static final StreamCodec<ByteBuf, HeatCapacitorData> STREAM_CODEC = StreamCodec.composite(
          ByteBufCodecs.DOUBLE, HeatCapacitorData::heat,
          ByteBufCodecs.DOUBLE, HeatCapacitorData::capacity,
          HeatCapacitorData::new
    );
}