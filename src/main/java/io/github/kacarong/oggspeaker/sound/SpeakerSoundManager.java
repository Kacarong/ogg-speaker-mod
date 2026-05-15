package io.github.kacarong.oggspeaker.sound;

import io.github.kacarong.oggspeaker.OggSpeakerMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Plays a sound for nearby clients with an explicit (fixed) attenuation range.
 *
 * <p>We build a fresh inline {@link SoundEvent} with {@link SoundEvent#createFixedRangeEvent}
 * so the client uses our range value for distance attenuation, then hand it to
 * {@link ServerLevel#playSeededSound} which performs the same broadcast vanilla
 * Minecraft uses for /playsound — so the audible-distance cutoff and packet
 * targeting are handled by the engine itself rather than by us approximating it.</p>
 */
public final class SpeakerSoundManager {
    private SpeakerSoundManager() {}

    public static boolean play(ServerLevel level, BlockPos pos, Identifier soundId,
                               float volume, float pitch, float rangeBlocks) {
        if (!BuiltInRegistries.SOUND_EVENT.containsKey(soundId)) {
            OggSpeakerMod.LOGGER.warn("[OGG Speaker] Sound '{}' is not registered; sending anyway via direct event.", soundId);
        }

        float fixedRange = Math.max(0.5f, rangeBlocks);
        // The client computes audible radius from fixedRange directly when present
        // (otherwise it falls back to max(volume, 1) * 16). To avoid the fallback
        // inflating the radius, clamp packet volume at 1.0 — beyond that, vanilla
        // MC scales radius rather than amplitude anyway.
        float packetVolume = Math.min(Math.max(volume, 0.0f), 1.0f);

        SoundEvent event = SoundEvent.createFixedRangeEvent(soundId, fixedRange);
        Holder<SoundEvent> holder = Holder.direct(event);

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        // Vanilla broadcast: handles distance cutoff using SoundEvent#getRange(volume)
        // — i.e. our fixedRange — and produces a proper ClientboundSoundPacket with
        // the inline SoundEvent so the client applies the same range for attenuation.
        level.playSeededSound(null, cx, cy, cz, holder, SoundSource.RECORDS, packetVolume, pitch, level.getRandom().nextLong());
        return true;
    }

    public static void stop(ServerLevel level, BlockPos pos, Identifier soundId, float rangeBlocks) {
        ClientboundStopSoundPacket packet = new ClientboundStopSoundPacket(soundId, SoundSource.RECORDS);
        double cx = pos.getX() + 0.5, cy = pos.getY() + 0.5, cz = pos.getZ() + 0.5;
        double r = Math.max(rangeBlocks, OggSpeakerMod.DEFAULT_RANGE_BLOCKS) + 8.0;
        double r2 = r * r;
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - cx;
            double dy = p.getY() - cy;
            double dz = p.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz <= r2) {
                p.connection.send(packet);
            }
        }
    }
}
