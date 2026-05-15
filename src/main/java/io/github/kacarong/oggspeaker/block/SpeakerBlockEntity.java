package io.github.kacarong.oggspeaker.block;

import com.mojang.serialization.Codec;
import io.github.kacarong.oggspeaker.OggSpeakerMod;
import io.github.kacarong.oggspeaker.registry.ModBlockEntities;
import io.github.kacarong.oggspeaker.sound.SpeakerSoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class SpeakerBlockEntity extends BlockEntity {
    @Nullable
    private Identifier lastSoundId;
    private float lastVolume = 1.0f;
    private float lastPitch = 1.0f;
    private float lastRange = OggSpeakerMod.DEFAULT_RANGE_BLOCKS;
    private boolean wasPowered = false;

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

    /** Called every server tick by SpeakerBlock#getTicker. Detects rising-edge redstone and replays last sound. */
    public void serverTick(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel sl)) return;
        boolean powered = level.hasNeighborSignal(pos);
        if (powered && !wasPowered && lastSoundId != null) {
            SpeakerSoundManager.play(sl, pos, lastSoundId, lastVolume, lastPitch, lastRange);
        }
        if (powered != wasPowered) {
            wasPowered = powered;
            setChanged();
        }
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
