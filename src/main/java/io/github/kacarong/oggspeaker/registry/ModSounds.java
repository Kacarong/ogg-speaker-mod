package io.github.kacarong.oggspeaker.registry;

import io.github.kacarong.oggspeaker.OggSpeakerMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Registers SOUND_SLOT_COUNT variable-range sound events under the resource-pack namespace. */
public final class ModSounds {
    /** Namespace used for sound assets. Resource packs override sounds under assets/{NAMESPACE}/sounds/. */
    public static final String SOUND_NAMESPACE = "musicquiz";

    private static final List<Identifier> SLOT_IDS = new ArrayList<>();
    private static final List<SoundEvent> SLOT_EVENTS = new ArrayList<>();

    private ModSounds() {}

    public static void register() {
        for (int i = 1; i <= OggSpeakerMod.SOUND_SLOT_COUNT; i++) {
            Identifier id = Identifier.fromNamespaceAndPath(SOUND_NAMESPACE, "slot" + i);
            SoundEvent event = SoundEvent.createVariableRangeEvent(id);
            Registry.register(BuiltInRegistries.SOUND_EVENT, id, event);
            SLOT_IDS.add(id);
            SLOT_EVENTS.add(event);
        }
    }

    public static List<Identifier> getSlotIds() { return Collections.unmodifiableList(SLOT_IDS); }
    public static List<SoundEvent> getSlotEvents() { return Collections.unmodifiableList(SLOT_EVENTS); }
}
