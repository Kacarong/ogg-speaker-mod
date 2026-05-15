package io.github.kacarong.oggspeaker.registry;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;

public final class ModItems {
    public static final ResourceKey<Item> SPEAKER_ITEM_KEY =
        ResourceKey.create(Registries.ITEM, ModBlocks.SPEAKER_ID);

    public static BlockItem SPEAKER_ITEM;

    private ModItems() {}

    public static void register() {
        SPEAKER_ITEM = new BlockItem(ModBlocks.SPEAKER_BLOCK, new Item.Properties().setId(SPEAKER_ITEM_KEY));
        Registry.register(BuiltInRegistries.ITEM, ModBlocks.SPEAKER_ID, SPEAKER_ITEM);

        // Add to the Functional Blocks creative tab.
        CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.FUNCTIONAL_BLOCKS).register(content -> content.accept(SPEAKER_ITEM));
    }
}
