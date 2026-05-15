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
 * Sends sound packets to nearby clients with an explicit (custom) attenuation range.
 *
 * <p>To honour a per-call range we build a <em>direct</em> SoundEvent holder with
 * {@link SoundEvent#createFixedRangeEvent} so the client uses our range instead of
 * the registered event's variable range.</p>
 */
public final class SpeakerSoundManager {
    private SpeakerSoundManager() {}

    public static boolean play(ServerLevel level, BlockPos pos, Identifier soundId,
                               float volume, float pitch, float rangeBlocks) {
        boolean registered = BuiltInRegistries.SOUND_EVENT.containsKey(soundId);
        if (!registered) {
            OggSpeakerMod.LOGGER.warn("[OGG Speaker] Sound '{}' is not registered; attempting to play anyway via direct event.", soundId);
        }

        SoundEvent event = SoundEvent.createFixedRangeEvent(soundId, Math.max(0.5f, rangeBlocks));
        Holder<SoundEvent> holder = Holder.direct(event);

        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;

        long seed = level.getRandom().nextLong();
        ClientboundSoundPacket packet = new ClientboundSoundPacket(holder, SoundSource.RECORDS, cx, cy, cz, volume, pitch, seed);

        double r = Math.max(rangeBlocks * Math.max(volume, 1.0f), rangeBlocks) + 4.0;
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
