package mekanism.client.gui.element.button;

import java.util.EnumMap;
import java.util.Map;
import java.util.function.Supplier;
import mekanism.api.text.EnumColor;
import mekanism.client.gui.IGuiWrapper;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.MekanismLang;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public class ColorButton extends MekanismButton {

    private static final Tooltip NONE = Tooltip.create(MekanismLang.NONE.translate());

    private final Map<EnumColor, Tooltip> tooltips = new EnumMap<>(EnumColor.class);
    private final Supplier<EnumColor> colorSupplier;

    public ColorButton(IGuiWrapper gui, int x, int y, int width, int height, Supplier<EnumColor> colorSupplier, @NotNull IClickable onPress, @NotNull IClickable onRightClick) {
        super(gui, x, y, width, height, Component.empty(), onPress, onRightClick);
        this.colorSupplier = colorSupplier;
    }

    @Override
    public void drawBackground(@NotNull GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTicks) {
        //Ensure the color gets reset. The default GuiButtonImage doesn't so other GuiButton's can have the color leak out of them
        EnumColor color = colorSupplier.get();
        if (color != null) {
            guiGraphics.fill(getButtonX(), getButtonY(), getButtonX() + getButtonWidth(), getButtonY() + getButtonHeight(), MekanismRenderer.getColorARGB(color, 1));
        }
    }

    @Override
    public void updateTooltip(int mouseX, int mouseY) {
        EnumColor color = colorSupplier.get();
        if (color != null) {
            setTooltip(tooltips.computeIfAbsent(color, c -> Tooltip.create(c.getColoredName())));
        } else {
            setTooltip(NONE);
        }
    }
}