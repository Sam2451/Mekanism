package mekanism.tools.common;

import java.util.concurrent.CompletableFuture;
import mekanism.common.BasePackMetadataGenerator;
import mekanism.common.MekanismDataGenerator;
import mekanism.tools.client.ToolsItemModelProvider;
import mekanism.tools.client.ToolsLangProvider;
import mekanism.tools.client.ToolsSpriteSourceProvider;
import mekanism.tools.common.recipe.ToolsRecipeProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataGenerator;
import net.minecraft.data.PackOutput;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.data.event.GatherDataEvent;

@EventBusSubscriber(modid = MekanismTools.MODID, bus = EventBusSubscriber.Bus.MOD)
public class ToolsDataGenerator {

    private ToolsDataGenerator() {
    }

    @SubscribeEvent
    public static void gatherData(GatherDataEvent event) {
        MekanismDataGenerator.bootstrapConfigs(MekanismTools.MODID);
        DataGenerator gen = event.getGenerator();
        PackOutput output = gen.getPackOutput();
        ExistingFileHelper existingFileHelper = event.getExistingFileHelper();
        CompletableFuture<HolderLookup.Provider> lookupProvider = event.getLookupProvider();
        gen.addProvider(true, new BasePackMetadataGenerator(output, ToolsLang.PACK_DESCRIPTION));
        //Client side data generators
        gen.addProvider(event.includeClient(), new ToolsLangProvider(output));
        gen.addProvider(event.includeClient(), new ToolsSpriteSourceProvider(output, existingFileHelper, lookupProvider));
        gen.addProvider(event.includeClient(), new ToolsItemModelProvider(output, existingFileHelper));
        //Server side data generators
        gen.addProvider(event.includeServer(), new ToolsTagProvider(output, lookupProvider, existingFileHelper));
        gen.addProvider(event.includeServer(), new ToolsRecipeProvider(output, lookupProvider, existingFileHelper));
        gen.addProvider(event.includeServer(), new ToolsAdvancementProvider(output, lookupProvider, existingFileHelper));
    }
}