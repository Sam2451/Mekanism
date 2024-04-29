package mekanism.client.gui.element.custom.module;

import java.util.function.Consumer;
import mekanism.api.gear.IModule;
import mekanism.api.gear.config.ModuleColorData;
import mekanism.client.gui.GuiModuleTweaker;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.scroll.GuiScrollList;
import mekanism.client.gui.element.text.GuiTextField;
import mekanism.client.gui.element.window.GuiColorWindow;
import mekanism.common.MekanismLang;
import mekanism.common.content.gear.Module;
import mekanism.common.content.gear.ModuleConfigItem;
import mekanism.common.content.gear.ModuleHelper;
import mekanism.common.content.gear.shared.ModuleColorModulationUnit;
import mekanism.common.lib.Color;
import mekanism.common.registries.MekanismModules;
import mekanism.common.util.text.TextUtils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

class ColorSelection extends MiniElement {

    private static final int OFFSET_Y = 1;
    private final int OFFSET_X;

    private final ModuleConfigItem<Integer> data;
    @Nullable
    private final GuiModuleTweaker.ArmorPreview armorPreview;
    private final boolean handlesAlpha;

    ColorSelection(GuiModuleScreen parent, ModuleConfigItem<Integer> data, int xPos, int yPos, boolean handlesAlpha, @Nullable GuiModuleTweaker.ArmorPreview armorPreview) {
        super(parent, xPos, yPos);
        this.data = data;
        this.handlesAlpha = handlesAlpha;
        this.armorPreview = armorPreview;
        OFFSET_X = this.parent.getScreenWidth() - 26;
    }

    private Color getColor() {
        //Note: We can use argb regardless of if it handles alpha as the color data
        return Color.argb(data.get());
    }

    @Override
    protected int getNeededHeight() {
        return 20;
    }

    @Override
    protected void renderBackground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int xTarget = getRelativeX() + OFFSET_X;
        int yTarget = getRelativeY() + OFFSET_Y;
        GuiUtils.drawOutline(guiGraphics, xTarget, yTarget, 18, 18, GuiTextField.SCREEN_COLOR.getAsInt());
        //Render the transparency grid inside it
        guiGraphics.blit(GuiColorWindow.TRANSPARENCY_GRID, xTarget + 1, yTarget + 1, 0, 0, 16, 16);
        //Draw color
        GuiUtils.fill(guiGraphics, xTarget + 1, yTarget + 1, 16, 16, data.get());
    }

    @Override
    protected void renderForeground(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        int textColor = parent.screenTextColor();
        parent.drawScaledTextScaledBound(guiGraphics, data.getDescription(), getRelativeX() + 3, getRelativeY(), textColor, this.parent.getScreenWidth() - 3 - GuiScrollList.TEXTURE_WIDTH, 0.8F);
        String hex;
        if (handlesAlpha) {
            hex = TextUtils.hex(false, 4, data.get());
        } else {
            hex = TextUtils.hex(false, 3, getColor().rgb());
        }
        parent.drawTextExact(guiGraphics, MekanismLang.GENERIC_HEX.translate(hex), getRelativeX() + 3, getRelativeY() + 11, textColor);
    }

    @Override
    protected void click(double mouseX, double mouseY) {
        if (mouseOver(mouseX, mouseY, OFFSET_X, OFFSET_Y, 18, 18)) {
            Consumer<Color> updatePreviewColor = null;
            Runnable previewReset = null;
            IModule<?> currentModule = parent.getCurrentModule();
            if (armorPreview != null && data.matches(MekanismModules.COLOR_MODULATION_UNIT, ModuleColorModulationUnit.COLOR_CONFIG_KEY) && currentModule != null) {
                ItemStack stack = currentModule.getContainer().getPreviewStack();
                if (stack.getItem() instanceof ArmorItem armorItem) {
                    Module<ModuleColorModulationUnit> module = ModuleHelper.get().getModule(stack, MekanismModules.COLOR_MODULATION_UNIT);
                    if (module != null) {
                        ModuleColorData configItem = module.getConfigItemData(ModuleColorModulationUnit.COLOR_CONFIG_KEY, ModuleColorData.class);
                        if (configItem != null) {
                            //Ensure the preview has been initialized
                            armorPreview.get();
                            EquipmentSlot slot = armorItem.getEquipmentSlot();
                            //Replace the current preview with our copy
                            armorPreview.updatePreview(slot, stack);
                            updatePreviewColor = c -> configItem.set(c.argb());
                            previewReset = () -> armorPreview.resetToDefault(slot);
                        }
                    }
                }
            }
            GuiColorWindow window = new GuiColorWindow(parent.gui(), parent.getGuiWidth() / 2 - 160 / 2, parent.getGuiHeight() / 2 - 120 / 2, handlesAlpha,
                  color -> setData(data, color.argb()), armorPreview, updatePreviewColor, previewReset);
            window.setColor(getColor());
            parent.gui().addWindow(window);
        }
    }
}