package mekanism.common.attachments.containers.chemical.gas;

import java.util.function.BiPredicate;
import java.util.function.LongSupplier;
import java.util.function.Predicate;
import java.util.function.Supplier;
import mekanism.api.AutomationType;
import mekanism.api.chemical.attribute.ChemicalAttributeValidator;
import mekanism.api.chemical.gas.Gas;
import mekanism.api.chemical.gas.GasStack;
import mekanism.api.chemical.gas.IGasTank;
import mekanism.common.attachments.containers.chemical.ComponentBackedChemicalTank;
import mekanism.common.registries.MekanismDataComponents;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ComponentBackedGasTank extends ComponentBackedChemicalTank<Gas, GasStack, AttachedGases> implements IGasTank {

    public ComponentBackedGasTank(ItemStack attachedTo, int tankIndex, BiPredicate<@NotNull Gas, @NotNull AutomationType> canExtract,
          BiPredicate<@NotNull Gas, @NotNull AutomationType> canInsert, Predicate<@NotNull Gas> validator, LongSupplier rate, LongSupplier capacity,
          @Nullable ChemicalAttributeValidator attributeValidator) {
        super(attachedTo, tankIndex, canExtract, canInsert, validator, rate, capacity, attributeValidator);
    }

    @Override
    protected Supplier<? extends DataComponentType<AttachedGases>> dataComponentType() {
        return MekanismDataComponents.ATTACHED_GASES;
    }
}