package mekanism.common.attachments.containers.energy;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import mekanism.api.AutomationType;
import mekanism.api.math.FloatingLongSupplier;
import mekanism.common.attachments.containers.creator.BaseContainerCreator;
import mekanism.common.attachments.containers.creator.IBasicContainerCreator;
import mekanism.common.capabilities.energy.BasicEnergyContainer;
import org.jetbrains.annotations.NotNull;

public class EnergyContainersBuilder {

    public static EnergyContainersBuilder builder() {
        return new EnergyContainersBuilder();
    }

    private final List<IBasicContainerCreator<? extends ComponentBackedEnergyContainer>> containerCreators = new ArrayList<>();

    private EnergyContainersBuilder() {
    }

    public BaseContainerCreator<AttachedEnergy, ComponentBackedEnergyContainer> build() {
        return new BaseEnergyContainerCreator(containerCreators);
    }

    public EnergyContainersBuilder addBasic(FloatingLongSupplier rate, FloatingLongSupplier maxEnergy) {
        return addContainer((type, attachedTo, containerIndex) -> new ComponentBackedEnergyContainer(attachedTo, containerIndex, BasicEnergyContainer.manualOnly,
              BasicEnergyContainer.alwaysTrue, rate, maxEnergy));
    }

    public EnergyContainersBuilder addBasic(Predicate<@NotNull AutomationType> canExtract, Predicate<@NotNull AutomationType> canInsert, FloatingLongSupplier rate,
          FloatingLongSupplier maxEnergy) {
        return addContainer((type, attachedTo, containerIndex) -> new ComponentBackedEnergyContainer(attachedTo, containerIndex, canExtract, canInsert, rate, maxEnergy));
    }

    public EnergyContainersBuilder addContainer(IBasicContainerCreator<? extends ComponentBackedEnergyContainer> capacitor) {
        containerCreators.add(capacitor);
        return this;
    }

    private static class BaseEnergyContainerCreator extends BaseContainerCreator<AttachedEnergy, ComponentBackedEnergyContainer> {

        public BaseEnergyContainerCreator(List<IBasicContainerCreator<? extends ComponentBackedEnergyContainer>> creators) {
            super(creators);
        }

        @Override
        public AttachedEnergy initStorage(int containers) {
            return new AttachedEnergy(containers);
        }
    }
}