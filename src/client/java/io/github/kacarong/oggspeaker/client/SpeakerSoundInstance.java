package io.github.kacarong.oggspeaker.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;

/**
 * Speaker-positioned sound that recomputes its own volume each client tick based on
 * straight-line distance from the local player to the speaker block. We do this
 * because relying on MC's built-in OpenAL distance attenuation (LINEAR / sounds.json
 * {@code attenuation_distance}) proved unreliable in this environment — sounds
 * kept playing at full volume regardless of distance.
 *
 * <p>Set to {@link Attenuation#NONE} so the sound engine does <em>not</em> apply
 * any distance falloff of its own; we override the {@code volume} field directly
 * in {@link #tick()}. The position fields {@code x/y/z} are still pinned to the
 * speaker so OpenAL's stereo panning still gives a sense of direction.</p>
 */
public final class SpeakerSoundInstance extends AbstractTickableSoundInstance {
    private final BlockPos speakerPos;
    private final float baseVolume;
    private final float range;

    public SpeakerSoundInstance(Identifier soundId, BlockPos pos, float baseVolume, float pitch, float range) {
        // Wrap our raw Identifier into a SoundEvent so AbstractTickableSoundInstance's
        // protected constructor accepts it. The fixedRange on the SoundEvent is irrelevant
        // because we use Attenuation.NONE and recompute volume ourselves.
        // Route through SoundSource.PLAYERS so the in-game "Players" volume slider controls
        // speaker output (rather than RECORDS, which is the jukebox/note-block slider).
        super(SoundEvent.createVariableRangeEvent(soundId), SoundSource.PLAYERS, RandomSource.create());
        this.speakerPos = pos.immutable();
        this.baseVolume = Math.max(0.0f, baseVolume);
        this.pitch = pitch;
        this.range = Math.max(1.0f, range);
        this.looping = false;
        this.delay = 0;
        this.relative = false;
        this.attenuation = SoundInstance.Attenuation.NONE;
        this.x = pos.getX() + 0.5;
        this.y = pos.getY() + 0.5;
        this.z = pos.getZ() + 0.5;
        // Seed an initial volume so the first frame isn't full-bore before tick() runs.
        this.volume = computeVolume();
    }

    @Override
    public void tick() {
        this.volume = computeVolume();
    }

    /**
     * Allow the sound to begin playing even when the per-tick volume is 0 (player is currently
     * outside {@code range} of the speaker). Without this, {@code SoundEngine#play} bails out
     * at start with {@code NOT_STARTED} for any speaker whose initial volume rounds to zero,
     * which broke the "place 2 speakers, one is far" case and also broke command-block
     * triggers where the operator isn't standing right next to a speaker.
     */
    @Override
    public boolean canStartSilent() {
        return true;
    }

    private float computeVolume() {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null) return 0.0f;
        Vec3 eye = player.getEyePosition();
        double dx = (speakerPos.getX() + 0.5) - eye.x;
        double dy = (speakerPos.getY() + 0.5) - eye.y;
        double dz = (speakerPos.getZ() + 0.5) - eye.z;
        double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
        float attenuation = (float) Math.max(0.0, Math.min(1.0, 1.0 - dist / range));
        return baseVolume * attenuation;
    }

    /** Externally request this instance to stop on its next tick. */
    public void requestStop() {
        super.stop();
    }

    public BlockPos getSpeakerPos() {
        return speakerPos;
    }
}
