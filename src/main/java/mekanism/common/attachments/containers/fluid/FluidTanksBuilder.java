package mekanism.common.attachments.containers.fluid;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import java.util.function.Predicate;
import mekanism.api.recipes.MekanismRecipe;
import mekanism.common.attachments.containers.ContainsRecipe;
import mekanism.common.attachments.containers.creator.BaseContainerCreator;
import mekanism.common.attachments.containers.creator.IBasicContainerCreator;
import mekanism.common.capabilities.fluid.BasicFluidTank;
import mekanism.common.config.MekanismConfig;
import mekanism.common.recipe.IMekanismRecipeTypeProvider;
import mekanism.common.recipe.lookup.cache.IInputRecipeCache;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jetbrains.annotations.NotNull;

public class FluidTanksBuilder {

    public static FluidTanksBuilder builder() {
        return new FluidTanksBuilder();
    }

    private final List<IBasicContainerCreator<? extends ComponentBackedFluidTank>> tankCreators = new ArrayList<>();

    private FluidTanksBuilder() {
    }

    public BaseContainerCreator<AttachedFluids, ComponentBackedFluidTank> build() {
        return new BaseFluidTankCreator(tankCreators);
    }

    public <RECIPE extends MekanismRecipe, INPUT_CACHE extends IInputRecipeCache> FluidTanksBuilder addBasic(int capacity,
          IMekanismRecipeTypeProvider<RECIPE, INPUT_CACHE> recipeType, ContainsRecipe<INPUT_CACHE, FluidStack> containsRecipe) {
        return addBasic(capacity, fluid -> containsRecipe.check(recipeType.getInputCache(), null, fluid));
    }

    public FluidTanksBuilder addBasic(int capacity, Predicate<@NotNull FluidStack> isValid) {
        return addBasic(() -> capacity, isValid);
    }

    public FluidTanksBuilder addBasic(IntSupplier capacity, Predicate<@NotNull FluidStack> isValid) {
        return addTank((type, attachedTo, containerIndex) -> new ComponentBackedFluidTank(attachedTo, containerIndex, BasicFluidTank.manualOnly, BasicFluidTank.alwaysTrueBi,
              isValid, MekanismConfig.general.fluidItemFillRate, capacity));
    }

    public FluidTanksBuilder addBasic(int capacity) {
        return addBasic(() -> capacity);
    }

    public FluidTanksBuilder addBasic(IntSupplier capacity) {
        return addTank((type, attachedTo, containerIndex) -> new ComponentBackedFluidTank(attachedTo, containerIndex, BasicFluidTank.manualOnly, BasicFluidTank.alwaysTrueBi,
              BasicFluidTank.alwaysTrue, MekanismConfig.general.fluidItemFillRate, capacity));
    }

    public FluidTanksBuilder addBasicExtractable(IntSupplier rate, IntSupplier capacity, Predicate<@NotNull FluidStack> isValid) {
        return addTank((type, attachedTo, containerIndex) -> new ComponentBackedFluidTank(attachedTo, containerIndex, BasicFluidTank.alwaysTrueBi, BasicFluidTank.alwaysTrueBi,
              isValid, rate, capacity));
    }

    public FluidTanksBuilder addTank(IBasicContainerCreator<? extends ComponentBackedFluidTank> tank) {
        tankCreators.add(tank);
        return this;
    }

    private static class BaseFluidTankCreator extends BaseContainerCreator<AttachedFluids, ComponentBackedFluidTank> {

        public BaseFluidTankCreator(List<IBasicContainerCreator<? extends ComponentBackedFluidTank>> creators) {
            super(creators);
        }

        @Override
        public AttachedFluids initStorage(int containers) {
            return new AttachedFluids(containers);
        }
    }
}