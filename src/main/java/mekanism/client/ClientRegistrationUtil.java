package mekanism.client;

import java.lang.ref.WeakReference;
import mekanism.api.providers.IBlockProvider;
import mekanism.api.providers.IItemProvider;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.machine.GuiAdvancedElectricMachine;
import mekanism.client.gui.machine.GuiElectricMachine;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.block.interfaces.IColoredBlock;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.item.interfaces.IColoredItem;
import mekanism.common.registration.impl.ContainerTypeRegistryObject;
import mekanism.common.registration.impl.FluidDeferredRegister;
import mekanism.common.registration.impl.TileEntityTypeRegistryObject;
import mekanism.common.registries.MekanismDataComponents;
import mekanism.common.tile.prefab.TileEntityAdvancedElectricMachine;
import mekanism.common.tile.prefab.TileEntityElectricMachine;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.gui.screens.MenuScreens.ScreenConstructor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.item.ItemProperties;
import net.minecraft.client.renderer.item.ItemPropertyFunction;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.FastColor;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.DynamicFluidContainerModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClientRegistrationUtil {

    private static final BlockColor COLORED_BLOCK_COLOR = (state, world, pos, tintIndex) -> {
        Block block = state.getBlock();
        if (block instanceof IColoredBlock coloredBlock) {
            return MekanismRenderer.getColorARGB(coloredBlock.getColor(), 1);
        }
        return -1;
    };
    private static final ItemColor COLORED_BLOCK_ITEM_COLOR = (stack, tintIndex) -> {
        Item item = stack.getItem();
        if (item instanceof BlockItem blockItem) {
            Block block = blockItem.getBlock();
            if (block instanceof IColoredBlock coloredBlock) {
                return MekanismRenderer.getColorARGB(coloredBlock.getColor(), 1);
            }
        }
        return -1;
    };
    private static final ItemColor COLORED_ITEM_COLOR = (stack, tintIndex) -> {
        Item item = stack.getItem();
        if (tintIndex == 1 && item instanceof IColoredItem) {
            EnumColor color = stack.get(MekanismDataComponents.COLOR);
            if (color == null) {
                return 0xFF555555;
            }
            int[] rgbCode = color.getRgbCode();
            return FastColor.ARGB32.color(255, rgbCode[0], rgbCode[1], rgbCode[2]);
        }
        return -1;
    };
    private static final ItemColor BUCKET_ITEM_COLOR = new DynamicFluidContainerModel.Colors();

    private ClientRegistrationUtil() {
    }

    @SafeVarargs
    public static <T extends BlockEntity> void bindTileEntityRenderer(EntityRenderersEvent.RegisterRenderers event, BlockEntityRendererProvider<T> rendererProvider,
          TileEntityTypeRegistryObject<? extends T>... tileEntityTypeROs) {
        if (tileEntityTypeROs.length == 0) {
            throw new IllegalArgumentException("No renderers provided.");
        } else if (tileEntityTypeROs.length == 1) {
            event.registerBlockEntityRenderer(tileEntityTypeROs[0].get(), rendererProvider);
        } else {
            BlockEntityRendererProvider<T> provider = new BlockEntityRendererProvider<>() {
                @Nullable
                private WeakReference<Context> cachedContext;
                @Nullable
                private WeakReference<BlockEntityRenderer<T>> cachedRenderer;

                @NotNull
                @Override
                public BlockEntityRenderer<T> create(@NotNull Context context) {
                    //If there is a cached context and renderer make use of it, otherwise create one and cache it
                    // this allows us to reduce the number of renderer classes we create
                    BlockEntityRenderer<T> renderer = cachedRenderer == null ? null : cachedRenderer.get();
                    if (cachedContext == null || cachedContext.get() != context || renderer == null) {
                        renderer = rendererProvider.create(context);
                        cachedContext = new WeakReference<>(context);
                        cachedRenderer = new WeakReference<>(renderer);
                    }
                    return renderer;
                }
            };
            for (TileEntityTypeRegistryObject<? extends T> tileTypeRO : tileEntityTypeROs) {
                event.registerBlockEntityRenderer(tileTypeRO.get(), provider);
            }
        }
    }

    public static void registerClientReloadListeners(RegisterClientReloadListenersEvent event, PreparableReloadListener... listeners) {
        for (PreparableReloadListener listener : listeners) {
            event.registerReloadListener(listener);
        }
    }

    public static <C extends AbstractContainerMenu, U extends Screen & MenuAccess<C>> void registerScreen(RegisterMenuScreensEvent event,
          ContainerTypeRegistryObject<C> type, ScreenConstructor<C, U> factory) {
        event.register(type.get(), factory);
    }

    //Helper method to register GuiElectricMachine due to generics not being able to be resolved through registerScreen
    @SuppressWarnings("RedundantTypeArguments")
    public static <TILE extends TileEntityElectricMachine, C extends MekanismTileContainer<TILE>> void registerElectricScreen(RegisterMenuScreensEvent event,
          ContainerTypeRegistryObject<C> type) {
        ClientRegistrationUtil.<C, GuiElectricMachine<TILE, C>>registerScreen(event, type, GuiElectricMachine::new);
    }

    //Helper method to register GuiAdvancedElectricMachine due to generics not being able to be resolved through registerScreen
    @SuppressWarnings("RedundantTypeArguments")
    public static <TILE extends TileEntityAdvancedElectricMachine, C extends MekanismTileContainer<TILE>> void registerAdvancedElectricScreen(RegisterMenuScreensEvent event,
          ContainerTypeRegistryObject<C> type) {
        ClientRegistrationUtil.<C, GuiAdvancedElectricMachine<TILE, C>>registerScreen(event, type, GuiAdvancedElectricMachine::new);
    }

    public static void registerKeyBindings(RegisterKeyMappingsEvent event, KeyMapping... keys) {
        for (KeyMapping key : keys) {
            event.register(key);
        }
    }

    public static void setPropertyOverride(IItemProvider itemProvider, ResourceLocation override, ItemPropertyFunction propertyGetter) {
        ItemProperties.register(itemProvider.asItem(), override, propertyGetter);
    }

    public static void registerItemColorHandler(RegisterColorHandlersEvent.Item event, ItemColor itemColor, IItemProvider... items) {
        for (IItemProvider itemProvider : items) {
            event.register(itemColor, itemProvider.asItem());
        }
    }

    public static void registerBlockColorHandler(RegisterColorHandlersEvent.Block event, BlockColor blockColor, IBlockProvider... blocks) {
        for (IBlockProvider blockProvider : blocks) {
            event.register(blockColor, blockProvider.getBlock());
        }
    }

    public static void registerBucketColorHandler(RegisterColorHandlersEvent.Item event, FluidDeferredRegister register) {
        for (Holder<Item> bucket : register.getBucketEntries()) {
            event.register(BUCKET_ITEM_COLOR, bucket.value());
        }
    }

    public static void registerIColoredBlockHandler(RegisterColorHandlersEvent event, IBlockProvider... blocks) {
        if (event instanceof RegisterColorHandlersEvent.Block blockEvent) {
            registerBlockColorHandler(blockEvent, COLORED_BLOCK_COLOR, blocks);
        } else if (event instanceof RegisterColorHandlersEvent.Item itemEvent) {
            registerItemColorHandler(itemEvent, COLORED_BLOCK_ITEM_COLOR, blocks);
        }
    }

    public static void registerIColoredItemHandler(RegisterColorHandlersEvent.Item event, IItemProvider... items) {
        registerItemColorHandler(event, COLORED_ITEM_COLOR, items);
    }
}