package mekanism.common.registration.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import mekanism.api.providers.IItemProvider;
import mekanism.common.attachments.IAttachmentAware;
import mekanism.common.attachments.containers.ContainerType;
import mekanism.common.attachments.containers.creator.IContainerCreator;
import mekanism.common.capabilities.ICapabilityAware;
import mekanism.common.config.IMekanismConfig;
import mekanism.common.registration.MekanismDeferredHolder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.util.INBTSerializable;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ItemRegistryObject<ITEM extends Item> extends MekanismDeferredHolder<Item, ITEM> implements IItemProvider {

    @Nullable
    private Map<ContainerType<?, ?, ?>, Supplier<? extends IContainerCreator<?, ?>>> defaultCreators;
    @Nullable
    private List<Consumer<RegisterCapabilitiesEvent>> containerCapabilities;

    public ItemRegistryObject(ResourceKey<Item> key) {
        super(key);
    }

    @NotNull
    @Override
    public ITEM asItem() {
        return value();
    }

    @Internal
    public <CONTAINER extends INBTSerializable<CompoundTag>> ItemRegistryObject<ITEM> addAttachmentOnlyContainers(ContainerType<CONTAINER, ?, ?> containerType,
          Supplier<IContainerCreator<? extends CONTAINER, ?>> defaultCreator) {
        //TODO - 1.20.5: Should we use Lazy instead of Supplier? As in theory the container creators are fine as is once made, they don't have to be reconstructed
        if (defaultCreators == null) {
            //In case any containers have deps on others make this linked even though it really shouldn't matter
            // as nothing should be trying to construct the containers between register calls
            defaultCreators = new LinkedHashMap<>();
        }
        if (defaultCreators.put(containerType, defaultCreator) != null) {
            throw new IllegalStateException("Duplicate attachments added for container type: " + containerType.getComponentName());
        }
        return this;
    }

    @Internal
    public <CONTAINER extends INBTSerializable<CompoundTag>> ItemRegistryObject<ITEM> addAttachedContainerCapabilities(ContainerType<CONTAINER, ?, ?> containerType,
          Supplier<IContainerCreator<? extends CONTAINER, ?>> defaultCreator, IMekanismConfig... requiredConfigs) {
        addAttachmentOnlyContainers(containerType, defaultCreator);
        return addContainerCapability(containerType, requiredConfigs);
    }

    @Internal
    private ItemRegistryObject<ITEM> addContainerCapability(ContainerType<?, ?, ?> containerType, IMekanismConfig... requiredConfigs) {
        if (containerCapabilities == null) {
            containerCapabilities = new ArrayList<>();
        }
        containerCapabilities.add(event -> containerType.registerItemCapabilities(event, asItem(), false, requiredConfigs));
        return this;
    }

    @Internal
    void registerCapabilities(RegisterCapabilitiesEvent event) {
        if (asItem() instanceof ICapabilityAware capabilityAware) {
            //TODO - 1.20.5: Re-evaluate how we are handling item capabilities. Should we cache the capability wrapper somewhere somehow?
            capabilityAware.attachCapabilities(event);
        }
        if (containerCapabilities != null) {
            for (Consumer<RegisterCapabilitiesEvent> consumer : containerCapabilities) {
                consumer.accept(event);
            }
            //We only allow registering once, and then we allow the memory to be freed up
            containerCapabilities = null;
        }
    }

    @Internal
    @SuppressWarnings({"unchecked", "rawtypes"})
    void attachDefaultContainers(IEventBus eventBus) {
        ITEM item = asItem();
        if (item instanceof IAttachmentAware attachmentAware) {
            attachmentAware.attachAttachments(eventBus);
        }
        if (defaultCreators != null) {
            for (Map.Entry<ContainerType<?, ?, ?>, Supplier<? extends IContainerCreator<?, ?>>> entry : defaultCreators.entrySet()) {
                //Note: We pass null for the event bus to not expose this attachment as a capability
                entry.getKey().addDefaultCreators(null, item, (Supplier) entry.getValue());
            }
            //We only allow them being attached once
            defaultCreators = null;
        }
    }
}