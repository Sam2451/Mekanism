package mekanism.client.render.tileentity;

import java.util.Random;
import javax.annotation.ParametersAreNonnullByDefault;
import com.google.common.base.Objects;
import com.mojang.blaze3d.matrix.MatrixStack;
import mekanism.client.render.effect.BillboardingEffectRenderer;
import mekanism.client.render.effect.BoltRenderer;
import mekanism.common.base.ProfilerConstants;
import mekanism.common.content.sps.SPSMultiblockData;
import mekanism.common.content.sps.SPSMultiblockData.CoilData;
import mekanism.common.lib.Color;
import mekanism.common.lib.effect.BoltEffect;
import mekanism.common.lib.effect.BoltEffect.BoltRenderInfo;
import mekanism.common.lib.effect.BoltEffect.SpawnFunction;
import mekanism.common.lib.effect.CustomEffect;
import mekanism.common.lib.math.Plane;
import mekanism.common.lib.math.voxel.VoxelCuboid.CuboidSide;
import mekanism.common.particle.custom.SPSOrbitEffect;
import mekanism.common.tile.multiblock.TileEntitySPSCasing;
import mekanism.common.util.MekanismUtils;
import mekanism.common.util.MekanismUtils.ResourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.IRenderTypeBuffer;
import net.minecraft.client.renderer.tileentity.TileEntityRendererDispatcher;
import net.minecraft.profiler.IProfiler;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@ParametersAreNonnullByDefault
public class RenderSPS extends MekanismTileEntityRenderer<TileEntitySPSCasing> {

    private static final CustomEffect CORE = new CustomEffect(MekanismUtils.getResource(ResourceType.RENDER, "energy_effect.png"));
    private static final Random rand = new Random();
    private static float MIN_SCALE = 0.1F, MAX_SCALE = 4F;

    private Minecraft minecraft = Minecraft.getInstance();
    private BoltRenderer bolts = new BoltRenderer();

    public RenderSPS(TileEntityRendererDispatcher renderer) {
        super(renderer);
        CORE.setColor(Color.rgba(255, 255, 255, 240));
    }

    @Override
    protected void render(TileEntitySPSCasing tile, float partialTick, MatrixStack matrix, IRenderTypeBuffer renderer, int light, int overlayLight, IProfiler profiler) {
        if (tile.isMaster && tile.getMultiblock().isFormed() && tile.getMultiblock().renderLocation != null && tile.getMultiblock().getBounds() != null) {
            Vec3d center = new Vec3d(tile.getMultiblock().getMinPos()).add(new Vec3d(tile.getMultiblock().getMaxPos())).add(new Vec3d(1, 1, 1)).scale(0.5);
            Vec3d renderCenter = center.subtract(tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
            if (!minecraft.isGamePaused()) {
                for (CoilData data : tile.getMultiblock().coilData.coilMap.values()) {
                    if (data.prevLevel > 0) {
                        bolts.update(data.coilPos.hashCode(), getBoltFromData(data, tile.getPos(), tile.getMultiblock(), renderCenter), partialTick);
                    }
                }
            }

            float energyScale = getEnergyScale(tile.getMultiblock().lastProcessed);
            int targetEffectCount = 0;

            if (!minecraft.isGamePaused() && !tile.getMultiblock().lastReceivedEnergy.isZero()) {
                if (rand.nextDouble() < getBoundedScale(energyScale, 0.01F, 0.4F)) {
                    CuboidSide side = CuboidSide.SIDES[rand.nextInt(6)];
                    Plane plane = Plane.getInnerCuboidPlane(tile.getMultiblock().getBounds(), side);
                    Vec3d endPos = plane.getRandomPoint(rand).subtract(tile.getPos().getX(), tile.getPos().getY(), tile.getPos().getZ());
                    BoltEffect bolt = new BoltEffect(BoltRenderInfo.ELECTRICITY, renderCenter, endPos, 15)
                          .size(0.01F * getBoundedScale(energyScale, 0.5F, 5))
                          .lifespan(8)
                          .spawn(SpawnFunction.NO_DELAY);
                    bolts.update(Objects.hashCode(side.hashCode(), endPos.hashCode()), bolt, partialTick);
                }
                targetEffectCount = (int) getBoundedScale(energyScale, 10, 120);
            }

            if (tile.orbitEffects.size() > targetEffectCount) {
                tile.orbitEffects.poll();
            } else if (tile.orbitEffects.size() < targetEffectCount && rand.nextDouble() < 0.5) {
                tile.orbitEffects.add(new SPSOrbitEffect(tile.getMultiblock(), center));
            }

            bolts.render(partialTick, matrix, renderer);

            tile.orbitEffects.forEach(effect -> BillboardingEffectRenderer.render(effect, tile.getPos(), matrix, renderer, tile.getWorld().getGameTime(), partialTick));

            if (tile.getMultiblock().lastProcessed > 0) {
                CORE.setPos(center);
                CORE.setScale(getBoundedScale(energyScale, MIN_SCALE, MAX_SCALE));
                BillboardingEffectRenderer.render(CORE, tile.getPos(), matrix, renderer, tile.getWorld().getGameTime(), partialTick);
            }
        }
    }

    private static float getEnergyScale(double lastProcessed) {
        return (float) Math.min(1, Math.max(0, (Math.log10(lastProcessed) + 2) / 4D));
    }

    private static float getBoundedScale(float scale, float min, float max) {
        return min + scale * (max - min);
    }

    private static BoltEffect getBoltFromData(CoilData data, BlockPos pos, SPSMultiblockData multiblock, Vec3d center) {
        Vec3d start = new Vec3d(data.coilPos.offset(data.side)).add(0.5, 0.5, 0.5);
        start = start.add(new Vec3d(data.side.getDirectionVec()).scale(0.5));
        int count = 1 + (data.prevLevel - 1) / 2;
        float size = 0.01F * data.prevLevel;
        return new BoltEffect(BoltRenderInfo.ELECTRICITY, start.subtract(pos.getX(), pos.getY(), pos.getZ()), center, 15)
              .count(count).size(size).lifespan(8).spawn(SpawnFunction.delay(4));
    }

    @Override
    protected String getProfilerSection() {
        return ProfilerConstants.SPS;
    }

    @Override
    public boolean isGlobalRenderer(TileEntitySPSCasing tile) {
        return tile.isMaster && tile.getMultiblock().isFormed() && tile.getMultiblock().renderLocation != null;
    }
}