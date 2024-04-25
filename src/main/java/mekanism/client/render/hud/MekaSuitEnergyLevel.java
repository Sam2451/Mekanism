package mekanism.client.render.hud;

import mekanism.api.energy.IEnergyContainer;
import mekanism.api.math.FloatingLong;
import mekanism.client.gui.GuiUtils;
import mekanism.client.gui.element.bar.GuiBar;
import mekanism.common.item.gear.ItemMekaSuitArmor;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import mekanism.common.util.StorageUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.LayeredDraw;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

public class MekaSuitEnergyLevel implements LayeredDraw.Layer {

    public static final MekaSuitEnergyLevel INSTANCE = new MekaSuitEnergyLevel();
    private static final ResourceLocation POWER_BAR = MekanismUtils.getResource(ResourceType.GUI_BAR, "horizontal_power_long.png");

    private MekaSuitEnergyLevel() {
    }

    @Override
    public void render(@NotNull GuiGraphics graphics, float partialTicks) {
        Minecraft minecraft = Minecraft.getInstance();
        FloatingLong capacity = FloatingLong.ZERO, stored = FloatingLong.ZERO;
        for (ItemStack stack : minecraft.player.getArmorSlots()) {
            if (stack.getItem() instanceof ItemMekaSuitArmor) {
                IEnergyContainer container = StorageUtils.getEnergyContainer(stack, 0);
                if (container != null) {
                    capacity = capacity.plusEqual(container.getMaxEnergy());
                    stored = stored.plusEqual(container.getEnergy());
                }
            }
        }
        if (!capacity.isZero()) {
            int x = graphics.guiWidth() / 2 - 91;
            int y = graphics.guiHeight() - minecraft.gui.leftHeight + 2;
            int length = (int) Math.round(stored.divide(capacity).doubleValue() * 79);
            GuiUtils.renderExtendedTexture(graphics, GuiBar.BAR, 2, 2, x, y, 81, 6);
            graphics.blit(POWER_BAR, x + 1, y + 1, length, 4, 0, 0, length, 4, 79, 4);
            minecraft.gui.leftHeight += 8;
        }
    }
}