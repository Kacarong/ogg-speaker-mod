package io.github.kacarong.oggspeaker.client;

import io.github.kacarong.oggspeaker.net.SpeakerPayloads;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side receiver: turns S2C payloads into actual SoundInstance plays
 * via {@link SoundManager#play} so positional attenuation is enforced by the
 * vanilla sound engine (linear, with distance from sounds.json).
 *
 * <p>Tracks the currently-playing SoundInstance per (speaker pos, sound id)
 * so /speaker stop can target the right instance.</p>
 */
public final class SpeakerClientSounds {
    private SpeakerClientSounds() {}

    /** Key = "x,y,z|namespace:path". Allows multiple speakers playing different sounds at once. */
    private static final Map<String, SoundInstance> ACTIVE = new ConcurrentHashMap<>();

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
        SoundInstance prev = ACTIVE.remove(key);
        if (prev != null) mc.getSoundManager().stop(prev);

        // Use the Identifier-based SimpleSoundInstance constructor that lets us pin
        // attenuation type to LINEAR and the absolute world position to the speaker block.
        SimpleSoundInstance instance = new SimpleSoundInstance(
            payload.sound(),
            SoundSource.RECORDS,
            payload.volume(),
            payload.pitch(),
            RandomSource.create(),
            false,                          // not looping
            0,                              // no delay
            SoundInstance.Attenuation.LINEAR,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            false                           // not listener-relative
        );
        mc.getSoundManager().play(instance);
        ACTIVE.put(key, instance);
    }

    private static void stopPlayback(Minecraft mc, SpeakerPayloads.Stop payload) {
        String key = keyOf(payload.pos(), payload.sound());
        SoundInstance instance = ACTIVE.remove(key);
        if (instance != null) {
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
