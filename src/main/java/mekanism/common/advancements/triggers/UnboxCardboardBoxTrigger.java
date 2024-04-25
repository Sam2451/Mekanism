package mekanism.common.advancements.triggers;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import mekanism.api.JsonConstants;
import mekanism.api.functions.ConstantPredicates;
import mekanism.common.advancements.MekanismCriteriaTriggers;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;

public class UnboxCardboardBoxTrigger extends SimpleCriterionTrigger<UnboxCardboardBoxTrigger.TriggerInstance> {

    @NotNull
    @Override
    public Codec<TriggerInstance> codec() {
        return TriggerInstance.CODEC;
    }

    public void trigger(ServerPlayer player) {
        this.trigger(player, ConstantPredicates.alwaysTrue());
    }

    public record TriggerInstance(Optional<ContextAwarePredicate> player) implements SimpleCriterionTrigger.SimpleInstance {

        public static final Codec<TriggerInstance> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                    EntityPredicate.ADVANCEMENT_CODEC.optionalFieldOf(JsonConstants.PLAYER).forGetter(TriggerInstance::player)
              ).apply(instance, TriggerInstance::new)
        );

        public static Criterion<TriggerInstance> unbox() {
            return MekanismCriteriaTriggers.UNBOX_CARDBOARD_BOX.createCriterion(new TriggerInstance(Optional.empty()));
        }
    }
}