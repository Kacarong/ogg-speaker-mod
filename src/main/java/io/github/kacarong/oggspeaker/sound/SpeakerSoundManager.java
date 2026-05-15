package io.github.kacarong.oggspeaker.sound;

import io.github.kacarong.oggspeaker.OggSpeakerMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ClientboundStopSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;

/**
 * Sends sound packets to nearby clients with an explicit (fixed) attenuation range.
 *
 * <p>The Minecraft client computes audible radius = fixedRange * max(volume, 1.0).
 * We therefore cap the packet volume at 1.0 so the user-supplied <em>range</em>
 * actually controls the audible distance, instead of being multiplied by a large
 * volume value (which made the sound audible across the entire world).</p>
 */
public final class SpeakerSoundManager {
    private SpeakerSoundManager() {}

    public static boolean play(ServerLevel level, BlockPos pos, Identifier soundId,
                               float volume, float pitch, float rangeBlocks) {
        boolean registered = BuiltInRegistries.SOUND_EVENT.containsKey(soundId);
        if (!registered) {
            OggSpeakerMod.LOGGER.warn("[OGG Speaker] Sound '{}' is not registered; sending anyway via direct event.", soundId);
        }

        float fixedRange = Math.max(0.5f, rangeBlocks);
        // Client-side audible radius = fixedRange * max(packetVolume, 1.0).
        // Cap packet volume at 1.0 so `range` is the true radius. The user's `volume`
        // beyond 1.0 has no useful effect in vanilla MC — extra amplitude requires
        // a louder OGG, not a bigger volume number — so we just clamp.
        float packetVolume = Math.min(Math.max(volume, 0.0f), 1.0f);

        SoundEvent event = SoundEvent.createFixedRangeEvent(soundId, fixedRange);
        Holder<SoundEvent> holder = Holder.direct(event);

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        long seed = level.getRandom().nextLong();
        ClientboundSoundPacket packet = new ClientboundSoundPacket(holder, SoundSource.RECORDS, cx, cy, cz, packetVolume, pitch, seed);

        // Send only to players within (range + small buffer). Anything farther
        // would compute zero amplitude on the client anyway and just waste bandwidth.
        double r = fixedRange + 4.0;
        double r2 = r * r;
        for (ServerPlayer p : level.players()) {
            double dx = p.getX() - cx;
            double dy = p.getY() - cy;
            double dz = p.getZ() - cz;
            if (dx * dx + dy * dy + dz * dz <= r2) {
                p.connection.send(packet);
            }
        }
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
