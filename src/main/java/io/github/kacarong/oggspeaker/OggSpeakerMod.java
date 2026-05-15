package io.github.kacarong.oggspeaker;

import io.github.kacarong.oggspeaker.command.SpeakerCommand;
import io.github.kacarong.oggspeaker.registry.ModBlockEntities;
import io.github.kacarong.oggspeaker.registry.ModBlocks;
import io.github.kacarong.oggspeaker.registry.ModItems;
import io.github.kacarong.oggspeaker.registry.ModSounds;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OggSpeakerMod implements ModInitializer {
    public static final String MOD_ID = "oggspeaker";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    /** Number of resource-pack overridable sound slots. */
    public static final int SOUND_SLOT_COUNT = 32;

    /** Default hearing range in blocks for the speaker block. */
    public static final float DEFAULT_RANGE_BLOCKS = 16.0f;

    @Override
    public void onInitialize() {
        ModSounds.register();
        ModBlocks.register();
        ModItems.register();
        ModBlockEntities.register();

        CommandRegistrationCallback.EVENT.register(
            (dispatcher, registryAccess, environment) -> SpeakerCommand.register(dispatcher)
        );

        LOGGER.info("[OGG Speaker] Initialized with {} sound slots.", SOUND_SLOT_COUNT);
    }
}
