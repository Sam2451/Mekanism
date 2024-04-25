package mekanism.common.network.to_server.button;

import io.netty.buffer.ByteBuf;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import mekanism.common.Mekanism;
import mekanism.common.MekanismLang;
import mekanism.common.item.interfaces.IGuiItem;
import mekanism.common.network.IMekanismPacket;
import mekanism.common.network.PacketUtils;
import mekanism.common.registries.MekanismContainerTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.util.ByIdMap;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.codec.NeoForgeStreamCodecs;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Used for informing the server that a click happened in a GUI and the gui window needs to change
 */
public record PacketItemButtonPress(ClickedItemButton buttonClicked, InteractionHand hand) implements IMekanismPacket {

    public static final CustomPacketPayload.Type<PacketItemButtonPress> TYPE = new CustomPacketPayload.Type<>(Mekanism.rl("item_button"));
    public static final StreamCodec<FriendlyByteBuf, PacketItemButtonPress> STREAM_CODEC = StreamCodec.composite(
          ClickedItemButton.STREAM_CODEC, PacketItemButtonPress::buttonClicked,
          PacketUtils.INTERACTION_HAND_STREAM_CODEC, PacketItemButtonPress::hand,
          PacketItemButtonPress::new
    );

    @NotNull
    @Override
    public CustomPacketPayload.Type<PacketItemButtonPress> type() {
        return TYPE;
    }

    @Override
    public void handle(IPayloadContext context) {
        Player player = context.player();
        ItemStack stack = player.getItemInHand(hand);
        if (stack.getItem() instanceof IGuiItem) {
            player.openMenu(buttonClicked.getProvider(stack, hand), buf -> {
                buf.writeEnum(hand);
                ItemStack.STREAM_CODEC.encode(buf, stack);
            });
        }
    }

    public enum ClickedItemButton {
        BACK_BUTTON((stack, hand) -> {
            if (stack.getItem() instanceof IGuiItem guiItem) {
                return guiItem.getContainerType().getProvider(stack.getHoverName(), hand, stack);
            }
            return null;
        }),
        QIO_FREQUENCY_SELECT((stack, hand) -> MekanismContainerTypes.QIO_FREQUENCY_SELECT_ITEM.getProvider(MekanismLang.QIO_FREQUENCY_SELECT, hand, stack));

        public static final IntFunction<ClickedItemButton> BY_ID = ByIdMap.continuous(ClickedItemButton::ordinal, values(), ByIdMap.OutOfBoundsStrategy.WRAP);
        public static final StreamCodec<ByteBuf, ClickedItemButton> STREAM_CODEC = ByteBufCodecs.idMapper(BY_ID, ClickedItemButton::ordinal);

        private final BiFunction<ItemStack, InteractionHand, @Nullable MenuProvider> providerFromItem;

        ClickedItemButton(BiFunction<ItemStack, InteractionHand, @Nullable MenuProvider> providerFromItem) {
            this.providerFromItem = providerFromItem;
        }

        @Nullable
        public MenuProvider getProvider(ItemStack stack, InteractionHand hand) {
            return providerFromItem.apply(stack, hand);
        }
    }
}