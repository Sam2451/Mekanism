package mekanism.tools.common;

import mekanism.api.providers.IItemProvider;
import mekanism.common.config.value.CachedFloatValue;
import mekanism.tools.common.config.MekanismToolsConfig;
import mekanism.tools.common.config.ToolsConfig.ArmorSpawnChanceConfig;
import mekanism.tools.common.registries.ToolsItems;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Drowned;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.Stray;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.monster.ZombifiedPiglin;
import net.minecraft.world.entity.monster.piglin.Piglin;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;

public class MobEquipmentHelper {

    private static final GearType REFINED_GLOWSTONE = new GearType(ToolsItems.REFINED_GLOWSTONE_SWORD, ToolsItems.REFINED_GLOWSTONE_SHOVEL,
          ToolsItems.REFINED_GLOWSTONE_HELMET, ToolsItems.REFINED_GLOWSTONE_CHESTPLATE, ToolsItems.REFINED_GLOWSTONE_LEGGINGS, ToolsItems.REFINED_GLOWSTONE_BOOTS,
          MekanismToolsConfig.tools.refinedGlowstoneSpawnRate);
    private static final GearType LAPIS_LAZULI = new GearType(ToolsItems.LAPIS_LAZULI_SWORD, ToolsItems.LAPIS_LAZULI_SHOVEL, ToolsItems.LAPIS_LAZULI_HELMET,
          ToolsItems.LAPIS_LAZULI_CHESTPLATE, ToolsItems.LAPIS_LAZULI_LEGGINGS, ToolsItems.LAPIS_LAZULI_BOOTS, MekanismToolsConfig.tools.lapisLazuliSpawnRate);
    private static final GearType REFINED_OBSIDIAN = new GearType(ToolsItems.REFINED_OBSIDIAN_SWORD, ToolsItems.REFINED_OBSIDIAN_SHOVEL, ToolsItems.REFINED_OBSIDIAN_HELMET,
          ToolsItems.REFINED_OBSIDIAN_CHESTPLATE, ToolsItems.REFINED_OBSIDIAN_LEGGINGS, ToolsItems.REFINED_OBSIDIAN_BOOTS, MekanismToolsConfig.tools.refinedObsidianSpawnRate);
    private static final GearType STEEL = new GearType(ToolsItems.STEEL_SWORD, ToolsItems.STEEL_SHOVEL, ToolsItems.STEEL_HELMET, ToolsItems.STEEL_CHESTPLATE,
          ToolsItems.STEEL_LEGGINGS, ToolsItems.STEEL_BOOTS, MekanismToolsConfig.tools.steelSpawnRate);
    private static final GearType BRONZE = new GearType(ToolsItems.BRONZE_SWORD, ToolsItems.BRONZE_SHOVEL, ToolsItems.BRONZE_HELMET, ToolsItems.BRONZE_CHESTPLATE,
          ToolsItems.BRONZE_LEGGINGS, ToolsItems.BRONZE_BOOTS, MekanismToolsConfig.tools.bronzeSpawnRate);
    private static final GearType OSMIUM = new GearType(ToolsItems.OSMIUM_SWORD, ToolsItems.OSMIUM_SHOVEL, ToolsItems.OSMIUM_HELMET, ToolsItems.OSMIUM_CHESTPLATE,
          ToolsItems.OSMIUM_LEGGINGS, ToolsItems.OSMIUM_BOOTS, MekanismToolsConfig.tools.osmiumSpawnRate);

    private static boolean isZombie(LivingEntity entity) {
        //Ignore the specific subclasses that can't spawn with armor in vanilla
        return entity instanceof Zombie && !(entity instanceof Drowned) && !(entity instanceof ZombifiedPiglin);
    }

    public static void onLivingSpecialSpawn(MobSpawnEvent.FinalizeSpawn event) {
        LivingEntity entity = event.getEntity();
        boolean isZombie = isZombie(entity);
        if (isZombie || entity instanceof Skeleton || entity instanceof Stray || entity instanceof Piglin) {
            //Don't bother calculating random numbers unless the instanceof checks pass
            RandomSource random = event.getLevel().getRandom();
            boolean isHard = event.getDifficulty().getDifficulty() == Difficulty.HARD;
            float difficultyMultiplier = event.getDifficulty().getSpecialMultiplier();
            GearType gearType = null;
            if (random.nextFloat() < MekanismToolsConfig.tools.armorSpawnChance.get() * difficultyMultiplier) {
                //We can only spawn refined glowstone armor on piglins
                gearType = getGearType(entity instanceof Piglin ? 0 : random.nextInt(6));
                setEntityArmorWithChance(random, entity, isHard, difficultyMultiplier, gearType);
            }
            if (isZombie) {
                CachedFloatValue spawnChance = isHard ? MekanismToolsConfig.tools.weaponSpawnChanceHard : MekanismToolsConfig.tools.weaponSpawnChance;
                if (random.nextFloat() < spawnChance.get()) {
                    if (gearType == null) {
                        gearType = getGearType(random.nextInt(6));
                    }
                    if (gearType.spawnChance.canSpawnWeapon.get()) {
                        IItemProvider weapon = random.nextFloat() < gearType.spawnChance.swordWeight.get() ? gearType.sword : gearType.shovel;
                        setStackIfEmpty(entity, random, gearType.spawnChance.weaponEnchantmentChance.get(), difficultyMultiplier, EquipmentSlot.MAINHAND, weapon);
                    }
                }
            }
        }
    }

    private static GearType getGearType(int type) {
        return switch (type) {
            default -> REFINED_GLOWSTONE;
            case 1 -> LAPIS_LAZULI;
            case 2 -> REFINED_OBSIDIAN;
            case 3 -> STEEL;
            case 4 -> BRONZE;
            case 5 -> OSMIUM;
        };
    }

    private static void setEntityArmorWithChance(RandomSource random, LivingEntity entity, boolean isHard, float difficultyMultiplier, GearType gearType) {
        ArmorSpawnChanceConfig chanceConfig = gearType.spawnChance();
        float stopChance = isHard ? chanceConfig.multiplePieceChanceHard.get() : chanceConfig.multiplePieceChance.get();
        if (random.nextFloat() < chanceConfig.bootsChance.get()) {
            setStackIfEmpty(entity, random, chanceConfig.armorEnchantmentChance.get(), difficultyMultiplier, EquipmentSlot.FEET, gearType.boots);
            if (random.nextFloat() < stopChance) {
                return;
            }
        }
        if (random.nextFloat() < chanceConfig.leggingsChance.get()) {
            setStackIfEmpty(entity, random, chanceConfig.armorEnchantmentChance.get(), difficultyMultiplier, EquipmentSlot.LEGS, gearType.leggings);
            if (random.nextFloat() < stopChance) {
                return;
            }
        }
        if (random.nextFloat() < chanceConfig.chestplateChance.get()) {
            setStackIfEmpty(entity, random, chanceConfig.armorEnchantmentChance.get(), difficultyMultiplier, EquipmentSlot.CHEST, gearType.chestplate);
            if (random.nextFloat() < stopChance) {
                return;
            }
        }
        if (random.nextFloat() < chanceConfig.helmetChance.get()) {
            setStackIfEmpty(entity, random, chanceConfig.armorEnchantmentChance.get(), difficultyMultiplier, EquipmentSlot.HEAD, gearType.helmet);
        }
    }

    private static void setStackIfEmpty(LivingEntity entity, RandomSource random, float baseChance, float difficultyMultiplier, EquipmentSlot slot, IItemProvider item) {
        if (entity.getItemBySlot(slot).isEmpty()) {
            ItemStack stack = item.getItemStack();
            if (random.nextFloat() < baseChance * difficultyMultiplier) {
                //Copy of vanilla's enchant item level logic
                //TODO - 1.20.5: Re-evaluate this and what feature flags we pass
                stack = EnchantmentHelper.enchantItem(FeatureFlags.VANILLA_SET, random, stack, (int) (5 + difficultyMultiplier * random.nextInt(18)), false);
            }
            entity.setItemSlot(slot, stack);
        }
    }

    private record GearType(IItemProvider sword, IItemProvider shovel, IItemProvider helmet, IItemProvider chestplate, IItemProvider leggings, IItemProvider boots,
                            ArmorSpawnChanceConfig spawnChance) {
    }
}