package mekanism.client.gui.robit;

import java.util.function.Supplier;
import mekanism.client.SpecialColors;
import mekanism.client.gui.GuiMekanism;
import mekanism.client.gui.element.GuiElement;
import mekanism.client.gui.element.GuiInnerScreen;
import mekanism.client.gui.element.GuiSideHolder;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.button.MekanismImageButton;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.window.GuiRobitRename;
import mekanism.client.gui.element.window.GuiRobitSkinSelect;
import mekanism.client.gui.element.window.GuiWindow;
import mekanism.common.MekanismLang;
import mekanism.common.entity.EntityRobit;
import mekanism.common.inventory.container.entity.robit.MainRobitContainer;
import mekanism.common.network.PacketUtils;
import mekanism.common.network.to_server.PacketGuiInteract;
import mekanism.common.network.to_server.PacketGuiInteract.GuiInteractionEntity;
import mekanism.common.network.to_server.button.PacketEntityButtonPress;
import mekanism.common.network.to_server.button.PacketEntityButtonPress.ClickedEntityButton;
import mekanism.common.util.text.EnergyDisplay;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import org.jetbrains.annotations.NotNull;

public class GuiRobitMain extends GuiMekanism<MainRobitContainer> {

    private final EntityRobit robit;
    private MekanismImageButton renameButton;
    private MekanismImageButton skinButton;

    public GuiRobitMain(MainRobitContainer container, Inventory inv, Component title) {
        super(container, inv, title);
        robit = container.getEntity();
        dynamicSlots = true;
        titleLabelX = 76;
    }

    private void openWindow(GuiWindow window, Supplier<? extends GuiElement> elementSupplier) {
        window.setListenerTab(elementSupplier);
        elementSupplier.get().active = false;
        addWindow(window);
    }

    @Override
    protected void addGuiElements() {
        super.addGuiElements();
        addRenderableWidget(new GuiSecurityTab(this, robit, 120));
        addRenderableWidget(GuiSideHolder.create(this, imageWidth, 6, 106, false, false, SpecialColors.TAB_ROBIT_MENU));
        addRenderableWidget(new GuiInnerScreen(this, 27, 16, 122, 56));
        addRenderableWidget(new GuiHorizontalPowerBar(this, robit.getEnergyContainer(), 27, 74, 120));
        addRenderableWidget(new MekanismImageButton(this, 6, 16, 18, getButtonLocation("home"), (element, mouseX, mouseY) -> {
            PacketUtils.sendToServer(new PacketGuiInteract(GuiInteractionEntity.GO_HOME, ((GuiRobitMain) element.gui()).robit));
            Minecraft.getInstance().setScreen(null);
            return true;
        })).setTooltip(MekanismLang.ROBIT_TELEPORT);
        renameButton = addRenderableWidget(new MekanismImageButton(this, 6, 35, 18, getButtonLocation("rename"), (element, mouseX, mouseY) -> {
            GuiRobitMain gui = (GuiRobitMain) element.gui();
            gui.openWindow(new GuiRobitRename(gui, 27, 16, gui.robit), () -> renameButton);
            return true;
        }));
        renameButton.setTooltip(MekanismLang.ROBIT_RENAME);
        skinButton = addRenderableWidget(new MekanismImageButton(this, 6, 54, 18, getButtonLocation("skin"), (element, mouseX, mouseY) -> {
            GuiRobitMain gui = (GuiRobitMain) element.gui();
            gui.openWindow(new GuiRobitSkinSelect(gui, 4, -12, gui.robit), () -> skinButton);
            return true;
        }));
        skinButton.setTooltip(MekanismLang.ROBIT_SKIN_SELECT);
        addRenderableWidget(new MekanismImageButton(this, 152, 35, 18, getButtonLocation("drop"),
              (element, mouseX, mouseY) -> PacketUtils.sendToServer(new PacketGuiInteract(GuiInteractionEntity.PICKUP_DROPS, ((GuiRobitMain) element.gui()).robit))))
              .setTooltip(MekanismLang.ROBIT_TOGGLE_PICKUP);
        addRenderableWidget(new MekanismImageButton(this, 152, 54, 18, getButtonLocation("follow"),
              (element, mouseX, mouseY) -> PacketUtils.sendToServer(new PacketGuiInteract(GuiInteractionEntity.FOLLOW, ((GuiRobitMain) element.gui()).robit))))
              .setTooltip(MekanismLang.ROBIT_TOGGLE_FOLLOW);
        addRenderableWidget(new MekanismImageButton(this, imageWidth + 3, 10, 18, getButtonLocation("main"), (element, mouseX, mouseY) -> {
            //Clicking main button doesn't do anything while already on the main GUI
            return true;
        })).setTooltip(MekanismLang.ROBIT);
        addRenderableWidget(new MekanismImageButton(this, imageWidth + 3, 30, 18, getButtonLocation("crafting"),
              (element, mouseX, mouseY) -> PacketUtils.sendToServer(new PacketEntityButtonPress(ClickedEntityButton.ROBIT_CRAFTING, ((GuiRobitMain) element.gui()).robit))))
              .setTooltip(MekanismLang.ROBIT_CRAFTING);
        addRenderableWidget(new MekanismImageButton(this, imageWidth + 3, 50, 18, getButtonLocation("inventory"),
              (element, mouseX, mouseY) -> PacketUtils.sendToServer(new PacketEntityButtonPress(ClickedEntityButton.ROBIT_INVENTORY, ((GuiRobitMain) element.gui()).robit))))
              .setTooltip(MekanismLang.ROBIT_INVENTORY);
        addRenderableWidget(new MekanismImageButton(this, imageWidth + 3, 70, 18, getButtonLocation("smelting"),
              (element, mouseX, mouseY) -> PacketUtils.sendToServer(new PacketEntityButtonPress(ClickedEntityButton.ROBIT_SMELTING, ((GuiRobitMain) element.gui()).robit))))
              .setTooltip(MekanismLang.ROBIT_SMELTING);
        addRenderableWidget(new MekanismImageButton(this, imageWidth + 3, 90, 18, getButtonLocation("repair"),
              (element, mouseX, mouseY) -> PacketUtils.sendToServer(new PacketEntityButtonPress(ClickedEntityButton.ROBIT_REPAIR, ((GuiRobitMain) element.gui()).robit))))
              .setTooltip(MekanismLang.ROBIT_REPAIR);
    }

    @Override
    protected void drawForegroundText(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY) {
        drawString(guiGraphics, title, titleLabelX, titleLabelY, titleTextColor());
        drawTextScaledBound(guiGraphics, MekanismLang.ROBIT_GREETING.translate(robit.getName()), 29, 18, screenTextColor(), 119);
        drawTextScaledBound(guiGraphics, MekanismLang.ENERGY.translate(EnergyDisplay.of(robit.getEnergyContainer())), 29, 36 - 4, screenTextColor(), 119);
        drawTextScaledBound(guiGraphics, MekanismLang.ROBIT_FOLLOWING.translate(robit.getFollowing()), 29, 45 - 4, screenTextColor(), 119);
        drawTextScaledBound(guiGraphics, MekanismLang.ROBIT_DROP_PICKUP.translate(robit.getDropPickup()), 29, 54 - 4, screenTextColor(), 119);
        CharSequence owner = robit.getOwnerName().length() > 14 ? robit.getOwnerName().subSequence(0, 14) : robit.getOwnerName();
        drawTextScaledBound(guiGraphics, MekanismLang.ROBIT_OWNER.translate(owner), 29, 63 - 4, screenTextColor(), 119);
        super.drawForegroundText(guiGraphics, mouseX, mouseY);
    }
}