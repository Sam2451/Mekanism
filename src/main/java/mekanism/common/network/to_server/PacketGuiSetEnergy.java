package mekanism.common.network.to_server;

import io.netty.buffer.ByteBuf;
import java.util.function.BiConsumer;
import java.util.function.IntFunction;
import mekanism.api.math.FloatingLong;
import mekanism.common.Mekanism;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import mekanism.common.tile.base.TileEntityMekanism;
import mekanism.common.tile.laser.TileEntityLaserAmplifier;
import mekanism.common.tile.machine.TileEntityResistiveHeater;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

public record PacketGuiSetEnergy(GuiEnergyValue interaction, BlockPos pos, FloatingLong value) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<PacketGuiSetEnergy> TYPE = new CustomPacketPayload.Type<>(Mekanism.rl("set_energy"));
    public static final StreamCodec<ByteBuf, PacketGuiSetEnergy> STREAM_CODEC = StreamCodec.composite(
          GuiEnergyValue.STREAM_CODEC, PacketGuiSetEnergy::interaction,
          BlockPos.STREAM_CODEC, PacketGuiSetEnergy::pos,
          FloatingLong.STREAM_CODEC, PacketGuiSetEnergy::value,
          PacketGuiSetEnergy::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<PacketGuiSetEnergy> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        if (PacketUtils.blockEntity(context, pos) instanceof TileEntityMekanism tile) {
            interaction.consume(tile, value);
        }
    }

    public enum GuiEnergyValue {
        MIN_THRESHOLD((tile, value) -> {
            if (tile instanceof TileEntityLaserAmplifier amplifier) {
                amplifier.setMinThresholdFromPacket(value);
            }
        }),
        MAX_THRESHOLD((tile, value) -> {
            if (tile instanceof TileEntityLaserAmplifier amplifier) {
                amplifier.setMaxThresholdFromPacket(value);
            }
        }),
        ENERGY_USAGE((tile, value) -> {
            if (tile instanceof TileEntityResistiveHeater heater) {
                heater.setEnergyUsageFromPacket(value);
            }
        });

        public static final IntFunction<GuiEnergyValue> BY_ID = ByIdMap.continuous(GuiEnergyValue::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, GuiEnergyValue> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, GuiEnergyValue::ordinal);

        private final BiConsumer<TileEntityMekanism, FloatingLong> consumerForTile;

        GuiEnergyValue(BiConsumer<TileEntityMekanism, FloatingLong> consumerForTile) {
            this.consumerForTile = consumerForTile;
        }

        public void consume(TileEntityMekanism tile, FloatingLong value) {
            consumerForTile.accept(tile, value);
        }
    }
}