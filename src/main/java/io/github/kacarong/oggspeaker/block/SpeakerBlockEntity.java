package io.github.kacarong.oggspeaker.block;

import com.mojang.serialization.Codec;
import io.github.kacarong.oggspeaker.OggSpeakerMod;
import io.github.kacarong.oggspeaker.registry.ModBlockEntities;
import io.github.kacarong.oggspeaker.sound.SpeakerSoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SpeakerBlockEntity extends BlockEntity {
    /** Per-level registry of loaded speaker positions, for global "/speaker play" broadcast. */
    private static final Map<ResourceKey<Level>, Set<BlockPos>> ACTIVE = new HashMap<>();

    @Nullable
    private Identifier lastSoundId;
    private float lastVolume = 1.0f;
    private float lastPitch = 1.0f;
    private float lastRange = OggSpeakerMod.DEFAULT_RANGE_BLOCKS;
    private boolean wasPowered = false;
    private boolean registered = false;

    public SpeakerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SPEAKER_BE, pos, state);
    }

    public void rememberLastPlayed(Identifier soundId, float volume, float pitch, float range) {
        this.lastSoundId = soundId;
        this.lastVolume = volume;
        this.lastPitch = pitch;
        this.lastRange = range;
        setChanged();
    }

    @Nullable
    public Identifier getLastSoundId() { return lastSoundId; }
    public float getLastVolume() { return lastVolume; }
    public float getLastPitch() { return lastPitch; }
    public float getLastRange() { return lastRange; }

    private void registerIfNeeded(ServerLevel sl) {
        if (registered) return;
        ACTIVE.computeIfAbsent(sl.dimension(), k -> new HashSet<>()).add(getBlockPos().immutable());
        registered = true;
    }

    private void unregister() {
        if (!registered) return;
        if (level instanceof ServerLevel sl) {
            Set<BlockPos> set = ACTIVE.get(sl.dimension());
            if (set != null) set.remove(getBlockPos());
        }
        registered = false;
    }

    @Override
    public void setRemoved() {
        unregister();
        super.setRemoved();
    }

    /** Called every server tick by SpeakerBlock#getTicker. Detects rising-edge redstone and replays last sound. */
    public void serverTick(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        registerIfNeeded(sl);
        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !wasPowered && lastSoundId != null) {
            SpeakerSoundManager.play(sl, pos, lastSoundId, lastVolume, lastPitch, lastRange);
        }
        if (powered != wasPowered) {
            wasPowered = powered;
            setChanged();
        }
    }

    /** Snapshot of all currently-loaded speaker positions in the given level. */
    public static Set<BlockPos> getAllPositions(ServerLevel level) {
        Set<BlockPos> set = ACTIVE.get(level.dimension());
        return set == null ? Collections.emptySet() : new HashSet<>(set);
    }

    @Override
    public void saveAdditional(ValueOutput data) {
        super.saveAdditional(data);
        if (lastSoundId != null) data.store("LastSound", Codec.STRING, lastSoundId.toString());
        data.store("LastVolume", Codec.FLOAT, lastVolume);
        data.store("LastPitch", Codec.FLOAT, lastPitch);
        data.store("LastRange", Codec.FLOAT, lastRange);
        data.store("WasPowered", Codec.BOOL, wasPowered);
    }

    @Override
    public void loadAdditional(ValueInput data) {
        super.loadAdditional(data);
        String s = data.read("LastSound", Codec.STRING).orElse(null);
        lastSoundId = (s == null) ? null : Identifier.tryParse(s);
        lastVolume = data.read("LastVolume", Codec.FLOAT).orElse(1.0f);
        lastPitch = data.read("LastPitch", Codec.FLOAT).orElse(1.0f);
        lastRange = data.read("LastRange", Codec.FLOAT).orElse(OggSpeakerMod.DEFAULT_RANGE_BLOCKS);
        wasPowered = data.read("WasPowered", Codec.BOOL).orElse(false);
    }
}
