package io.github.kacarong.oggspeaker.registry;

import io.github.kacarong.oggspeaker.block.SpeakerBlockEntity;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static BlockEntityType<SpeakerBlockEntity> SPEAKER_BE;

    private ModBlockEntities() {}

    public static void register() {
        SPEAKER_BE = FabricBlockEntityTypeBuilder.create(SpeakerBlockEntity::new, ModBlocks.SPEAKER_BLOCK).build();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ModBlocks.SPEAKER_ID, SPEAKER_BE);
    }
}
