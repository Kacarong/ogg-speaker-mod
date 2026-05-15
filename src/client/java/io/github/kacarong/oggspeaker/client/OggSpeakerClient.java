package io.github.kacarong.oggspeaker.client;

import net.fabricmc.api.ClientModInitializer;

public class OggSpeakerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // No client-only setup yet. Sound playback is handled via vanilla packets.
    }
}
