package mekanism.common.inventory.container.item;

import mekanism.api.security.IItemSecurityUtils;
import mekanism.common.attachments.qio.PortableQIODashboardInventory;
import mekanism.common.content.qio.IQIOCraftingWindowHolder;
import mekanism.common.inventory.container.QIOItemViewerContainer;
import mekanism.common.inventory.container.slot.HotBarSlot;
import mekanism.common.inventory.container.sync.SyncableItemStack;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_server.PacketItemGuiInteract;
import mekanism.common.network.to_server.PacketItemGuiInteract.ItemGuiInteraction;
import mekanism.common.registries.MekanismContainerTypes;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class PortableQIODashboardContainer extends QIOItemViewerContainer {

    protected final InteractionHand hand;
    protected ItemStack stack;

    private PortableQIODashboardContainer(int id, Inventory inv, InteractionHand hand, ItemStack stack, boolean remote, IQIOCraftingWindowHolder craftingWindowHolder) {
        super(MekanismContainerTypes.PORTABLE_QIO_DASHBOARD, id, inv, remote, craftingWindowHolder);
        this.hand = hand;
        this.stack = stack;
        addSlotsAndOpen();
    }

    public PortableQIODashboardContainer(int id, Inventory inv, InteractionHand hand, ItemStack stack, boolean remote) {
        this(id, inv, hand, stack, remote, new PortableQIODashboardInventory(inv.player.level(), stack));
    }

    public InteractionHand getHand() {
        return hand;
    }

    public ItemStack getStack() {
        return stack;
    }

    @Override
    public PortableQIODashboardContainer recreate() {
        PortableQIODashboardContainer container = new PortableQIODashboardContainer(containerId, inv, hand, stack, true, craftingWindowHolder);
        sync(container);
        return container;
    }

    @Override
    protected void addInventorySlots(@NotNull Inventory inv) {
        super.addInventorySlots(inv);
        if (offhandSlots.isEmpty()) {
            //If we don't have a slot relating to offhand data, add a syncable itemstack to track any changes that might happen to the stack
            // as some of them may need to be reflected in the GUI https://github.com/mekanism/Mekanism/issues/7923
            track(SyncableItemStack.create(inv.player::getOffhandItem, item -> {
                inv.player.setItemSlot(EquipmentSlot.OFFHAND, item);
                if (hand == InteractionHand.OFF_HAND && stack.is(item.getItem())) {
                    stack = item;
                }
            }));
        }
    }

    @Override
    protected HotBarSlot createHotBarSlot(@NotNull Inventory inv, int index, int x, int y) {
        // special handling to prevent removing the dashboard from the player's inventory slot
        if (index == inv.selected && hand == InteractionHand.MAIN_HAND) {
            return new HotBarSlot(inv, index, x, y) {
                @Override
                public boolean mayPickup(@NotNull Player player) {
                    return false;
                }

                @Override
                public void set(@NotNull ItemStack item) {
                    super.set(item);
                    if (stack.is(item.getItem())) {
                        //Track changes to the main hand's slot
                        stack = item;
                    }
                }
            };
        }
        return super.createHotBarSlot(inv, index, x, y);
    }

    @Override
    public void clicked(int slotId, int dragType, @NotNull ClickType clickType, @NotNull Player player) {
        if (clickType == ClickType.SWAP) {
            if (hand == InteractionHand.OFF_HAND && dragType == 40) {
                //Block pressing f to swap it when it is in the offhand
                return;
            } else if (hand == InteractionHand.MAIN_HAND && dragType >= 0 && dragType < Inventory.getSelectionSize()) {
                //Block taking out of the selected slot (we don't validate we have a hotbar slot as we always should for this container)
                if (!hotBarSlots.get(dragType).mayPickup(player)) {
                    return;
                }
            }
        }
        super.clicked(slotId, dragType, clickType, player);
    }

    @Override
    public boolean canPlayerAccess(@NotNull Player player) {
        return IItemSecurityUtils.INSTANCE.canAccess(player, stack);
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return !this.stack.isEmpty() && player.getItemInHand(this.hand).is(this.stack.getItem());
    }

    @Override
    public boolean shiftClickIntoFrequency() {
        //Shouldn't be empty but validate it
        return !this.stack.isEmpty() && stack.getOrDefault(MekanismDataComponents.INSERT_INTO_FREQUENCY, true);
    }

    @Override
    public void toggleTargetDirection() {
        //Change the data client side so that it is reflected in the gui as we don't handle updating client side data
        PacketUtils.sendToServer(new PacketItemGuiInteract(ItemGuiInteraction.TARGET_DIRECTION_BUTTON, this.hand));
        //stack.set(MekanismDataComponents.INSERT_INTO_FREQUENCY, !stack.getData(MekanismDataComponents.INSERT_INTO_FREQUENCY));
    }
}
