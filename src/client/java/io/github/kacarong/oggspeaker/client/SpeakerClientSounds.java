package io.github.kacarong.oggspeaker.client;

import io.github.kacarong.oggspeaker.net.SpeakerPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side receiver: turns S2C payloads into actual SoundInstance plays
 * via {@link net.minecraft.client.sounds.SoundManager#play} using a custom
 * {@link SpeakerSoundInstance} which handles distance attenuation per tick
 * (we don't trust OpenAL's built-in LINEAR distance model here — it kept
 * playing sounds at full volume regardless of distance).
 *
 * <p>Tracks the currently-playing SpeakerSoundInstance per (speaker pos, sound id)
 * so /speaker stop can target the right instance.</p>
 */
public final class SpeakerClientSounds {
    private SpeakerClientSounds() {}

    /** Key = "x,y,z|namespace:path". Allows multiple speakers playing different sounds at once. */
    private static final Map<String, SpeakerSoundInstance> ACTIVE = new ConcurrentHashMap<>();

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(SpeakerPayloads.Play.TYPE, (payload, ctx) -> {
            Minecraft mc = ctx.client();
            mc.execute(() -> startPlayback(mc, payload));
        });

        ClientPlayNetworking.registerGlobalReceiver(SpeakerPayloads.Stop.TYPE, (payload, ctx) -> {
            Minecraft mc = ctx.client();
            mc.execute(() -> stopPlayback(mc, payload));
        });
    }

    private static void startPlayback(Minecraft mc, SpeakerPayloads.Play payload) {
        BlockPos pos = payload.pos();
        // Stop a previous instance at this position+sound, if any, so retriggers don't stack.
        String key = keyOf(pos, payload.sound());
        SpeakerSoundInstance prev = ACTIVE.remove(key);
        if (prev != null) {
            prev.requestStop();
            mc.getSoundManager().stop(prev);
        }

        SpeakerSoundInstance instance = new SpeakerSoundInstance(
            payload.sound(),
            pos,
            payload.volume(),
            payload.pitch(),
            payload.range()
        );
        mc.getSoundManager().play(instance);
        ACTIVE.put(key, instance);
    }

    private static void stopPlayback(Minecraft mc, SpeakerPayloads.Stop payload) {
        String key = keyOf(payload.pos(), payload.sound());
        SpeakerSoundInstance instance = ACTIVE.remove(key);
        if (instance != null) {
            instance.requestStop();
            mc.getSoundManager().stop(instance);
        } else {
            // Fallback: stop any instance of this sound id in the RECORDS channel
            mc.getSoundManager().stop(payload.sound(), SoundSource.RECORDS);
        }
    }

    private static String keyOf(BlockPos pos, net.minecraft.resources.Identifier id) {
        return pos.getX() + "," + pos.getY() + "," + pos.getZ() + "|" + id;
    }
}
