package mekanism.client.render.tileentity;

import com.mojang.blaze3d.matrix.MatrixStack;
import com.mojang.blaze3d.platform.GlStateManager;
import javax.annotation.Nonnull;
import mekanism.client.model.ModelSolarNeutronActivator;
import mekanism.client.render.MekanismRenderer;
import mekanism.common.tile.TileEntitySolarNeutronActivator;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.renderer.IRenderTypeBuffer;

public class RenderSolarNeutronActivator extends MekanismTileEntityRenderer<TileEntitySolarNeutronActivator> {

    private ModelSolarNeutronActivator model = new ModelSolarNeutronActivator();

    @Override
    public void func_225616_a_(@Nonnull TileEntitySolarNeutronActivator tile, float partialTick, @Nonnull MatrixStack matrix, @Nonnull IRenderTypeBuffer renderer, int light, int otherLight) {
        GlStateManager.pushMatrix();
        GlStateManager.translatef((float) x + 0.5F, (float) y + 1.5F, (float) z + 0.5F);
        bindTexture(MekanismUtils.getResource(ResourceType.RENDER, "solar_neutron_activator.png"));
        MekanismRenderer.rotate(tile.getDirection(), 0, 180, 90, 270);
        GlStateManager.rotatef(180, 0, 0, 1);
        setLightmapDisabled(true);
        model.render(0.0625F);
        setLightmapDisabled(false);
        GlStateManager.popMatrix();
    }

    @Override
    public boolean isGlobalRenderer(TileEntitySolarNeutronActivator tile) {
        return true;
    }
}