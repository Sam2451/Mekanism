package mekanism.api.recipes.ingredients.creator;

import com.mojang.serialization.Codec;
import java.util.stream.Stream;
import mekanism.api.annotations.NothingNullByDefault;
import mekanism.api.recipes.ingredients.InputIngredient;
import net.minecraft.core.Holder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.tags.TagKey;
import org.jetbrains.annotations.NotNull;

@NothingNullByDefault
public interface IIngredientCreator<TYPE, STACK, INGREDIENT extends InputIngredient<@NotNull STACK>> {

    /**
     * Creates an Ingredient that matches a given stack.
     *
     * @param instance Stack to match.
     *
     * @throws NullPointerException     if the given instance is null.
     * @throws IllegalArgumentException if the given instance is empty.
     */
    INGREDIENT from(STACK instance);

    /**
     * Creates an Ingredient that matches a provided type and amount.
     *
     * @param instance Type to match.
     * @param amount   Amount needed.
     *
     * @throws NullPointerException     if the given instance is null.
     * @throws IllegalArgumentException if the given instance is empty or an amount smaller than one.
     */
    INGREDIENT from(TYPE instance, int amount);

    /**
     * Creates an Ingredient that matches a provided type and amount.
     *
     * @param instance Type to match.
     * @param amount   Amount needed.
     *
     * @throws NullPointerException     if the given instance is null.
     * @throws IllegalArgumentException if the given instance is empty or an amount smaller than one.
     * @since 10.5.0
     */
    default INGREDIENT fromHolder(Holder<TYPE> instance, int amount) {
        return from(instance.value(), amount);
    }

    /**
     * Creates an Ingredient that matches a given tag and amount.
     *
     * @param tag    Tag to match.
     * @param amount Amount needed.
     *
     * @throws NullPointerException     if the given tag is null.
     * @throws IllegalArgumentException if the given amount smaller than one.
     */
    INGREDIENT from(TagKey<TYPE> tag, int amount);

    /**
     * Retrieve a codec which can (de)encode a single or multi ingredient of this type.
     *
     * @return a codec for this ingredient type
     */
    Codec<INGREDIENT> codec();

    //TODO - 1.20.5: Docs
    StreamCodec<RegistryFriendlyByteBuf, INGREDIENT> streamCodec();

    /**
     * Combines multiple Ingredients into a single Ingredient.
     *
     * @param ingredients Ingredients to combine.
     *
     * @return Combined Ingredient.
     *
     * @throws NullPointerException     if the given array is null.
     * @throws IllegalArgumentException if the given array is empty.
     */
    @SuppressWarnings("unchecked")
    INGREDIENT createMulti(INGREDIENT... ingredients);

    /**
     * Creates an Ingredient out of a stream of Ingredients.
     *
     * @param ingredients Ingredient(s) to combine.
     *
     * @return Given Ingredient or Combined Ingredient if multiple were in the stream.
     *
     * @throws NullPointerException     if the given stream is null.
     * @throws IllegalArgumentException if the given stream is empty.
     */
    INGREDIENT from(Stream<INGREDIENT> ingredients);
}