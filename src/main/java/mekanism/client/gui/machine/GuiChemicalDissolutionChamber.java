package mekanism.client.gui.machine;

import mekanism.client.gui.GuiMekanismTile;
import mekanism.client.gui.element.bar.GuiHorizontalPowerBar;
import mekanism.client.gui.element.gauge.GaugeType;
import mekanism.client.gui.element.gauge.GuiGasGauge;
import mekanism.client.gui.element.gauge.GuiMergedChemicalTankGauge;
import mekanism.client.gui.element.progress.GuiProgress;
import mekanism.client.gui.element.progress.ProgressType;
import mekanism.client.gui.element.tab.GuiEnergyTab;
import mekanism.client.gui.element.tab.GuiRedstoneControlTab;
import mekanism.client.gui.element.tab.GuiSecurityTab;
import mekanism.client.gui.element.tab.GuiSideConfigurationTab;
import mekanism.client.gui.element.tab.GuiTransporterConfigTab;
import mekanism.client.gui.element.tab.GuiUpgradeTab;
import mekanism.common.MekanismLang;
import mekanism.common.inventory.container.tile.MekanismTileContainer;
import mekanism.common.tile.machine.TileEntityChemicalDissolutionChamber;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.util.text.ITextComponent;

public class GuiChemicalDissolutionChamber extends GuiMekanismTile<TileEntityChemicalDissolutionChamber, MekanismTileContainer<TileEntityChemicalDissolutionChamber>> {

    public GuiChemicalDissolutionChamber(MekanismTileContainer<TileEntityChemicalDissolutionChamber> container, PlayerInventory inv, ITextComponent title) {
        super(container, inv, title);
        dynamicSlots = true;
    }

    @Override
    public void init() {
        super.init();
        addButton(new GuiSecurityTab<>(this, tile));
        addButton(new GuiRedstoneControlTab(this, tile));
        addButton(new GuiUpgradeTab(this, tile));
        addButton(new GuiHorizontalPowerBar(this, tile.getEnergyContainer(), 115, 75));
        addButton(new GuiSideConfigurationTab(this, tile));
        addButton(new GuiTransporterConfigTab(this, tile));
        addButton(new GuiEnergyTab(tile.getEnergyContainer(), this));
        addButton(new GuiGasGauge(() -> tile.injectTank, () -> tile.getGasTanks(null), GaugeType.STANDARD, this, 7, 4));
        addButton(new GuiMergedChemicalTankGauge<>(() -> tile.outputTank, () -> tile, GaugeType.STANDARD, this, 131, 13));
        addButton(new GuiProgress(tile::getScaledProgress, ProgressType.LARGE_RIGHT, this, 64, 40).jeiCategory(tile));
    }

    @Override
    protected void drawGuiContainerForegroundLayer(int mouseX, int mouseY) {
        drawTitleText(MekanismLang.CHEMICAL_DISSOLUTION_CHAMBER_SHORT.translate(), 4);
        super.drawGuiContainerForegroundLayer(mouseX, mouseY);
    }
}