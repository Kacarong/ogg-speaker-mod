package io.github.kacarong.oggspeaker.net;

import io.github.kacarong.oggspeaker.OggSpeakerMod;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Custom S2C payloads. We bypass vanilla {@link net.minecraft.network.protocol.game.ClientboundSoundPacket}
 * so the client can construct a {@link net.minecraft.client.resources.sounds.SimpleSoundInstance}
 * at the speaker's exact block position and let the OpenAL engine handle linear
 * distance attenuation directly — this proved more reliable than relying on the
 * vanilla sound packet, which in testing did not always attenuate.
 */
public final class SpeakerPayloads {
    private SpeakerPayloads() {}

    public record Play(Identifier sound, BlockPos pos, float volume, float pitch) implements CustomPacketPayload {
        // NOTE: CustomPacketPayload.createType(String) calls Identifier.withDefaultNamespace,
        // which in this MC version *does not* parse "namespace:path" — it treats the whole
        // string as the path under "minecraft" and an embedded ':' fails assertValidPath,
        // crashing the mod's main entrypoint. Build the Type directly with an explicit
        // namespaced Identifier to avoid that path.
        public static final CustomPacketPayload.Type<Play> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OggSpeakerMod.MOD_ID, "play"));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Play> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Play::sound,
            BlockPos.STREAM_CODEC, Play::pos,
            ByteBufCodecs.FLOAT, Play::volume,
            ByteBufCodecs.FLOAT, Play::pitch,
            Play::new
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    public record Stop(Identifier sound, BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<Stop> TYPE =
            new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(OggSpeakerMod.MOD_ID, "stop"));
        public static final StreamCodec<io.netty.buffer.ByteBuf, Stop> CODEC = StreamCodec.composite(
            Identifier.STREAM_CODEC, Stop::sound,
            BlockPos.STREAM_CODEC, Stop::pos,
            Stop::new
        );

        @Override
        public CustomPacketPayload.Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Registers the S2C codecs. Safe to call on both sides; the registry handles it. */
    public static void registerCommon() {
        PayloadTypeRegistry.clientboundPlay().register(Play.TYPE, Play.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(Stop.TYPE, Stop.CODEC);
    }
}
