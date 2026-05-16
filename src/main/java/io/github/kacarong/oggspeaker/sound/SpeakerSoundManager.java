package io.github.kacarong.oggspeaker.sound;

import io.github.kacarong.oggspeaker.OggSpeakerMod;
import io.github.kacarong.oggspeaker.net.SpeakerPayloads;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Server-side sound dispatcher. Sends a custom payload to every player in the
 * speaker's dimension so they can play the sound locally at the speaker's
 * block position with linear distance attenuation handled by the client's
 * sound engine (which uses {@code attenuation_distance} from sounds.json).
 */
public final class SpeakerSoundManager {
    private SpeakerSoundManager() {}

    public static boolean play(ServerLevel level, BlockPos pos, Identifier soundId,
                               float volume, float pitch, float rangeBlocks) {
        if (!BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
            OggSpeakerMod.LOGGER.warn("[OGG Speaker] Sound '{}' is not registered; sending anyway — client will need an entry in sounds.json.", soundId);
        }
        // Don't cap volume — client handles distance attenuation itself and clamps to 1.0
        // per-tick. Allowing >1.0 here lets users push the source gain (within reason) and
        // we forward `range` so the client knows how far the falloff should reach.
        float packetVolume = Math.max(volume, 0.0f);
        float packetRange = Math.max(rangeBlocks, 1.0f);
        SpeakerPayloads.Play payload = new SpeakerPayloads.Play(soundId, pos.immutable(), packetVolume, pitch, packetRange);
        for (ServerPlayer p : level.players()) {
            ServerPlayNetworking.send(p, payload);
        }
        return true;
    }

    public static void stop(ServerLevel level, BlockPos pos, Identifier soundId, float rangeBlocks) {
        SpeakerPayloads.Stop payload = new SpeakerPayloads.Stop(soundId, pos.immutable());
        for (ServerPlayer p : level.players()) {
            ServerPlayNetworking.send(p, payload);
        }
    }
}
