package mekanism.additions.common;

import java.util.function.Supplier;
import mekanism.additions.client.AdditionsClient;
import mekanism.additions.common.block.BlockObsidianTNT;
import mekanism.additions.common.config.MekanismAdditionsConfig;
import mekanism.additions.common.entity.baby.EntityBabyStray;
import mekanism.additions.common.registries.AdditionsBiomeModifierSerializers;
import mekanism.additions.common.registries.AdditionsBlocks;
import mekanism.additions.common.registries.AdditionsCreativeTabs;
import mekanism.additions.common.registries.AdditionsDataComponents;
import mekanism.additions.common.registries.AdditionsEntityTypes;
import mekanism.additions.common.registries.AdditionsItems;
import mekanism.additions.common.registries.AdditionsSounds;
import mekanism.additions.common.registries.AdditionsStructureModifierSerializers;
import mekanism.additions.common.voice.VoiceServerManager;
import mekanism.common.Mekanism;
import mekanism.common.base.IModModule;
import mekanism.common.config.MekanismModConfig;
import mekanism.common.lib.Version;
import net.minecraft.core.BlockPos;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.DefaultDispenseItemBehavior;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.config.ModConfigEvent;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.SpawnPlacementRegisterEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import org.jetbrains.annotations.NotNull;

@Mod(MekanismAdditions.MODID)
public class MekanismAdditions implements IModModule {

    public static final String MODID = "mekanismadditions";

    public static MekanismAdditions instance;

    /**
     * MekanismTools version number
     */
    public final Version versionNumber;

    /**
     * The VoiceServer manager for walkie-talkies
     */
    public static VoiceServerManager voiceManager;

    public MekanismAdditions(ModContainer modContainer, IEventBus modEventBus) {
        Mekanism.addModule(instance = this);
        MekanismAdditionsConfig.registerConfigs(modContainer);
        NeoForge.EVENT_BUS.addListener(this::serverStarting);
        NeoForge.EVENT_BUS.addListener(this::serverStopping);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::onConfigLoad);
        modEventBus.addListener(this::spawnPlacements);
        AdditionsDataComponents.DATA_COMPONENTS.register(modEventBus);
        AdditionsItems.ITEMS.register(modEventBus);
        AdditionsBlocks.BLOCKS.register(modEventBus);
        AdditionsCreativeTabs.CREATIVE_TABS.register(modEventBus);
        AdditionsEntityTypes.ENTITY_TYPES.register(modEventBus);
        AdditionsSounds.SOUND_EVENTS.register(modEventBus);
        AdditionsBiomeModifierSerializers.BIOME_MODIFIER_SERIALIZERS.register(modEventBus);
        AdditionsStructureModifierSerializers.STRUCTURE_MODIFIER_SERIALIZERS.register(modEventBus);

        //Set our version number to match the neoforge.mods.toml file, which matches the one in our build.gradle
        versionNumber = new Version(modContainer);
    }

    public static ResourceLocation rl(String path) {
        return new ResourceLocation(MekanismAdditions.MODID, path);
    }

    @Override
    public Version getVersion() {
        return versionNumber;
    }

    @Override
    public String getName() {
        return "Additions";
    }

    @Override
    public void resetClient() {
        AdditionsClient.reset();
    }

    @Override
    public void launchClient() {
        AdditionsClient.launch();
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            //Dispenser behavior
            DispenserBlock.registerBehavior(AdditionsBlocks.OBSIDIAN_TNT, new DefaultDispenseItemBehavior() {
                @NotNull
                @Override
                protected ItemStack execute(@NotNull BlockSource source, @NotNull ItemStack stack) {
                    BlockPos blockpos = source.pos().relative(source.state().getValue(DispenserBlock.FACING));
                    if (BlockObsidianTNT.createAndAddEntity(source.level(), blockpos, null)) {
                        source.level().gameEvent(null, GameEvent.ENTITY_PLACE, blockpos);
                        stack.shrink(1);
                        return stack;
                    }
                    //Otherwise, if something went very wrong, eject it as a normal item
                    return super.execute(source, stack);
                }
            });
        });
        Mekanism.logger.info("Loaded 'Mekanism: Additions' module.");
    }

    //TODO - 1.20.5: Do we want to move the spawn placements to being registered as part of mek entity type creation like we do for attributes
    private void spawnPlacements(SpawnPlacementRegisterEvent event) {
        //Setup some stuff related to entities
        //Register spawn controls for the baby entities based on the vanilla spawn controls
        registerSpawnControls(event, AdditionsEntityTypes.BABY_CREEPER, AdditionsEntityTypes.BABY_ENDERMAN, AdditionsEntityTypes.BABY_SKELETON,
              AdditionsEntityTypes.BABY_WITHER_SKELETON);
        //Slightly different restrictions for the baby stray, as strays have a slightly different spawn restriction
        event.register(AdditionsEntityTypes.BABY_STRAY.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, EntityBabyStray::spawnRestrictions,
              SpawnPlacementRegisterEvent.Operation.AND);
    }

    @SafeVarargs
    private static void registerSpawnControls(SpawnPlacementRegisterEvent event, Supplier<? extends EntityType<? extends Monster>>... entityTypeROs) {
        for (Supplier<? extends EntityType<? extends Monster>> entityTypeRO : entityTypeROs) {
            event.register(entityTypeRO.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, Monster::checkMonsterSpawnRules,
                  SpawnPlacementRegisterEvent.Operation.AND);
        }
    }

    private void serverStarting(ServerStartingEvent event) {
        if (MekanismAdditionsConfig.additions.voiceServerEnabled.get()) {
            if (voiceManager == null) {
                voiceManager = new VoiceServerManager();
            }
            voiceManager.start();
        }
    }

    private void serverStopping(ServerStoppingEvent event) {
        if (voiceManager != null) {
            voiceManager.stop();
            voiceManager = null;
        }
    }

    private void onConfigLoad(ModConfigEvent configEvent) {
        ModConfig config = configEvent.getConfig();
        //Make sure it is for the same modid as us
        if (config.getModId().equals(MODID) && config instanceof MekanismModConfig mekConfig) {
            mekConfig.clearCache(configEvent);
        }
    }
}