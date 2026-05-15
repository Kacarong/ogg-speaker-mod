package io.github.kacarong.oggspeaker.registry;

import io.github.kacarong.oggspeaker.OggSpeakerMod;
import io.github.kacarong.oggspeaker.block.SpeakerBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;

public final class ModBlocks {
    public static final Identifier SPEAKER_ID =
        Identifier.fromNamespaceAndPath(OggSpeakerMod.MOD_ID, "speaker");
    public static final ResourceKey<Block> SPEAKER_BLOCK_KEY =
        ResourceKey.create(Registries.BLOCK, SPEAKER_ID);

    public static SpeakerBlock SPEAKER_BLOCK;

    private ModBlocks() {}

    public static void register() {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
            .mapColor(MapColor.COLOR_BLACK)
            .strength(1.5f, 6.0f)
            .sound(SoundType.STONE)
            .requiresCorrectToolForDrops()
            .setId(SPEAKER_BLOCK_KEY);

        SPEAKER_BLOCK = new SpeakerBlock(props);
        Registry.register(BuiltInRegistries.BLOCK, SPEAKER_ID, SPEAKER_BLOCK);
    }
}
