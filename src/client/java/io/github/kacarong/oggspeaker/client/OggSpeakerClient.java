package io.github.kacarong.oggspeaker.client;

import net.fabricmc.api.ClientModInitializer;

public class OggSpeakerClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        SpeakerClientSounds.register();
    }
}
